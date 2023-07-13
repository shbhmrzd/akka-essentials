package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer

object ChildActorsExercise extends App {
  // Distributed word counting

  /*
  A master which is initialized with nChildren parameter
  It will create those many children and delegate the word count task to them
   */

  /*
    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Akka is awesome" to wordCounterMaster
      wcm will send a WordCountTask("..") to one of its children
        child replies with  a WordCountReplay(3) to the master
      master replies with 3 to the sender


      requester(outside actor) -> wcm -> wcw
                                r <- wcm  <-

     */
  // round robin logic for load distribution

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(text: String)
    case class WordCountTaskWorker(masterRef: ActorRef, text: String)
    case class WordCountReply(text: String, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    var childrenList : scala.collection.mutable.ListBuffer[ActorRef] = ListBuffer.empty[ActorRef]
    var idx = 0
    override def receive: Receive = {
      case Initialize(n) =>
        for (i <- 1 to n) {
          childrenList += context.actorOf(Props[WordCounterWorker])
        }
//        (1 to n).map(childrenList += context.actorOf(Props[WordCounterWorker]))
      case WordCountTask(word) =>
        if (childrenList.nonEmpty) {
          println(s"[master] delegating wc for $word to child - $idx")
          childrenList(idx) forward WordCountTaskWorker(context.self, word)
          idx = (idx + 1) % childrenList.length
        } else {
          // Handle case when no child actors are available
          println("[master] No child actors available")
        }
      case WordCountReply(word, res) =>
        println(s"[master] - wc for $word is $res")
        sender() ! res
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTaskWorker(master, word) =>
        println(s"${self.path} processing word count for $word")
        master forward  WordCountReply(word, word.split(" ").length)
    }
  }

  object RequestActor {
    case class RequestWordCount(master: ActorRef, word: String)
  }
  class RequestActor extends Actor {
    import RequestActor._
    import WordCounterMaster._
    def receive : Receive = {
      case n : Int => println(s"[RequestActor] got result $n")
      case RequestWordCount(master, word) => master ! WordCountTask(word)
    }
  }




  import WordCounterMaster._
  import RequestActor._
  val system = ActorSystem("WordCounter")
  val wordCounterMaster = system.actorOf(Props[WordCounterMaster])
  val requestActor = system.actorOf(Props[RequestActor])

  wordCounterMaster ! Initialize(3)

  Thread.sleep(600)

  requestActor ! RequestWordCount(wordCounterMaster, "Hello I am here")

  requestActor ! RequestWordCount(wordCounterMaster, "Hello akka is not here")
  requestActor ! RequestWordCount(wordCounterMaster, "wow is here")
  requestActor ! RequestWordCount(wordCounterMaster, "tlb backend datalogic akka kafka")


}
