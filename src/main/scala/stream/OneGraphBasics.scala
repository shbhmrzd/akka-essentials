package stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration.DurationInt

object OneGraphBasics extends App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer

  val input = Source(1 to 1000)
  val incrementer =  Flow[Int].map(x => x+1)
  val multiplier =  Flow[Int].map(x => x*10)

  // execute both above flows in parallel and return a tuple of their results
  val output = Sink.foreach[(Int,Int)](println)

  // step1 - setting up fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators in scope

      // step -2 add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int,Int]) // fan in operator

      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output
      // step4 return a closed shape
      ClosedShape  // FREEZE the builder's shape
      // shape object
    } // static graph
  ) // runnable graph

//  graph.run() // run the graph and materialize it


  /*
  Feed a single source into 2 sinks at the same time
  (hint: use a broadcast)
   */

  val singleSource = Source(1 to 10)

  val sink1 = Sink.foreach[Int](x => println(s"sink1 - $x"))
  val sink2 = Sink.foreach[Int](x => println(s"sink2 - $x"))

  val twoSinkRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))
      singleSource ~> broadcast
      broadcast.out(0) ~> sink1
      broadcast.out(1) ~> sink2

      /*
      implicit port numbering - not same as implicits in scala
      input ~> broadcast ~> sink1
            broadcast ~> sink2
       */
      ClosedShape
    }
  )

//  twoSinkRunnableGraph.run()

  /*
  Two sources fast and slow and both go into fan into shap Merge and then it goes to a fan out balance component
  which then sends the messages equally to two sink
   */

  val slowSource = Source(1 to 10).throttle(2, 1 second)
  val fastSource = Source(1 to 10).throttle(4, 1 second)
  val firstSink = Sink.fold[Int,Int](0)((count,element) => {
    println(s"Sink1 number of elements : $count")
    count+1
  })
  val secondSink = Sink.fold[Int, Int](0)((count, element) => {
    println(s"Sink2 number of elements : $count")
    count + 1
  })

  val mergeBalanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val mergeComponent = builder.add(Merge[Int](2))
      val balanceComponent = builder.add(Balance[Int](2))

      slowSource ~> mergeComponent;
      fastSource ~> mergeComponent;
      mergeComponent ~> balanceComponent;
      balanceComponent ~> firstSink
      balanceComponent ~> secondSink

      /*
      fastSource ~> mergeComponent ~> balanceComponent ~> sink1
      slowSource ~> mergeComponent ; balanceComponent ~> sink2
       */
      ClosedShape
    }
  )

  mergeBalanceGraph.run()
}
