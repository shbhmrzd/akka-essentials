package stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object ThreeMoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
  max3 operator
  - 3 inputs of type int
  - the maximum of 3
   */

  val max3StaticGraph = GraphDSL.create() {implicit builder =>
    import GraphDSL.Implicits._

    // step-2 define aux shapes

    val max1 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))

    // step 3
    max1.out ~> max2.in0

    // step4
    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink =  Sink.foreach[Int](x => println(s"max is $x"))

  // step 1
  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        // step 2 declare shapes
        val max3Shape = builder.add(max3StaticGraph)

        // step-3 tie
        source1 ~> max3Shape.in(0)
        source2 ~> max3Shape.in(1)
        source3 ~> max3Shape.in(2)
        max3Shape.out ~> maxSink

        // step 4
        ClosedShape
    }
  )
//  max3RunnableGraph.run()

  // same for UniformFanOutShape
  /*
  Uniform input because all input have same type
  Similarly uniform output because all output have same type
   */

  /*
  Non uniform fan out shape

  Processing bank transactions
    Txn suspicious if amount > 10000

    Streams component for txns
    - output1 let the transaction go through (txn)
    - output2 suspicious txn ids  (string)
   */

  case class Transaction(id:String, source:String, recipient: String, amount: Int, date:Date)
  val transactionSource = Source(List(
    Transaction("1","paul","jim",100, new Date),
    Transaction("2","daniel","jim",100001, new Date),
    Transaction("3","jim","alice",700, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious transaction Id : $txnId"))

  // step 1
  val suspiciousTxnStaticGraph = GraphDSL.create(){
    implicit builder =>
      import GraphDSL.Implicits._

      // step2 - define shapes
      val broadcast =  builder.add(Broadcast[Transaction](2))
      val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 1000))
      val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

      broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

      new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){
      implicit builder =>
        import GraphDSL.Implicits._

        val suspiciousTxnShape =  builder.add(suspiciousTxnStaticGraph)


        transactionSource ~> suspiciousTxnShape.in
        suspiciousTxnShape.out0 ~> bankProcessor
        suspiciousTxnShape.out1 ~> suspiciousAnalysisService
        ClosedShape
    }
  )
  suspiciousTxnRunnableGraph.run
}
