package stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object TwoOpenGraphs extends App {

  implicit val system = ActorSystem("Opengraphs")
  implicit val materializer = ActorMaterializer
  /*
  Earlier we worked with Runnable graphs which can be run directly
  Also called as closed graphs

  But we can also create open graphs using source, flow or sink
  So we get a source, flow and sink correspondingly
  which can later be tied together to make a runnable graph
   */


  /*
  A composite source that concatenates 2 sources
  - emits all the elements from the first source
  - then all the elements from the second source
   */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  // step 1
  val sourceGraph = Source.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      // step 2 declaring components
      val concat = builder.add(Concat[Int](2))

      // step3 tying them together
      firstSource ~> concat
      secondSource ~> concat
      // step 4
      SourceShape(concat.out)
    }
  )

//  sourceGraph.to(Sink.foreach(println)).run

  /*
  Complex sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1 :$x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  // step1
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create(){
      implicit builder =>

        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))

        broadcast ~> sink1
        broadcast ~> sink2

        SinkShape(broadcast.in)
    }
  )

//  firstSource.to(sinkGraph).run

  /*
  Complex flow
  write your own flow that's composed of two other flows
  - one that adds 1 to a number
  - one that does number * 10
   */

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)

  // step 1
  val flowGraph = Flow.fromGraph(
    GraphDSL.create(){ implicit builder =>

      import GraphDSL.Implicits._

      // everything operates on SHAPES
      // step 2 -  define auxiliary components
      // step 3 - connect the components
      /*
      this does not work, as only shapes can be joined using this ~> operator
      so we need to copy shapes of their
      incrementer ~> multiplier
       */
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)
      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out) // Shape
    } // static graph
  )

  firstSource.via(flowGraph).to(Sink.foreach(println)).run
}
