package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehaviour.Counter.{Decrement, Increment, Print}

object ChangingActorBehaviour extends App {



  object FussyKid {
    // we are using case object as this does not need any argument
    // below momstart needs kid ref so we made case class
    // so the responses can be made as [case object] -> think of it as using for enum use cases (my understanding)
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {

    import FussyKid._
    import Mom._

    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if(state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  /*
  Earlier we used a stateful fussykid where a var is used to store the state
  this is mutable state var and we wish to change that

  Now we keep two handlers and depending on the state the handler is changed
  but we dont store state directly, instead each of the handlers change the receive function definition
  using a function context.become(<receiveName>)
   */
  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive

    def happyReceive : Receive = {
      case Food(VEGETABLE) => context.become(sadReceive) // change my receive handler to sadReceive
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive : Receive = {
      case Food(VEGETABLE) =>
      case Food(CHOCOLATE) => context.become(happyReceive) // change my receive handler to happyReceive
      case Ask(_) => sender() ! KidReject
    }
  }


  object Mom {
    case class MomStart(kidRef: ActorRef)
    // this is saying Food is in Mom's domain
    // so we put it in the companion object of Mom
    // This is a good way to couple messages which are specific to an actor
    case class Food(food: String)
    case class Ask(message : String) // do you want to play ?

    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play ?")
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad, but at least he is healthy ")
    }
  }

  val system = ActorSystem("changingActorBehaviourDemo")
  val fussyKid = system.actorOf(Props[FussyKid])
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])

  mom ! Mom.MomStart(fussyKid)
  mom ! Mom.MomStart(statelessFussyKid)

    /*
    mom receives MomStart
      kid receives Food(VEGETABLE) -> kid will change the handler to sadReceive
      kid receives Ask(play ?) -> kid replies with the sadReceive handler
        mom receives KidReject
     */



  /*
  Exercises

  1. Recreate the counter actor with context become and no mutable state
   */
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }
  class Counter extends  Actor {
    import Counter._
    override def receive: Receive = countReceive(0)

    /*
    If we want to rewrite a stateful actor into a stateless actor
    we need to move the variable which was storing the state into a parameter to the
    function and use that as the state
     */
    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[countReceive]($currentCount) incrementing")
        context.become(countReceive(currentCount+1))
      case Decrement =>
        println(s"[countReceive]($currentCount) decrementing")
        context.become(countReceive(currentCount-1))
      case Print => println(s"[counter] my current count is $currentCount")
    }

  }

  val counter = system.actorOf(Props[Counter], "myCounter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /*
  2. Simplified voting system
   */



  case class Vote(candidate: String)
  case object VoteStatusRequest

  case class VoteStatusReply(candidate : Option[String])

  class Citizen extends Actor {
    var candidate : Option[String] = None
    override def receive: Receive = {
      case Vote(c) => candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {
    var stillWaiting : Set[ActorRef] = Set()
    var currentStatus : Map[String, Int] = Map()
    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) =>
        // citizen hasnt voted yet
        sender() ! VoteStatusRequest // this might end up in an infinite loop
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting  = stillWaiting - sender()
        val currentVotesOfCandidate = currentStatus.getOrElse(candidate, 0) + 1
        currentStatus += (candidate -> currentVotesOfCandidate)
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats : $currentStatus")
        }else{
          stillWaiting = newStillWaiting
        }
    }

    def voteCount(map : scala.collection.mutable.Map[String, Integer]) : Receive = {
      case name: String =>
        map.put(name, map.get(name).get + 1)
        context.become(voteCount(map))
    }
  }


  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Roland")
  charlie ! Vote("Jonas")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /*
  Print the status of the votes

  Martin -> 1
  Jonas -> 1
  Ronald -> 2
   */
}
