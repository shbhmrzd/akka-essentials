package part4faulttolerance

import akka.actor.typed.Terminated
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props}

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    // parent stops itself
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))

      case StopChild(name) =>
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))

      case Stop => context.stop(self)

      case message => log.info(message.toString)

    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /*
  method #1 - using context.stop
   */
  import Parent._

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")

  // to confirm child is created, use ActorSelection
  val child =  system.actorSelection("/user/parent/child1")
  child ! "hi kid!!"

  /*
  context.stop is non blocking
  that means the actor is not ended immediately
  so if we send a bunch of msgs some would go to the child and later we start seeing dead letter as no actor to receive msg
   */
  parent ! StopChild("child1")
  for(i <- 1 to 50) child ! s"are you still there - ${i}"

  /*
  Also if an actor is stopped, all its child actors are also stopped
   */
  parent ! StartChild("child2")
  val child2 = system.actorSelection("user/parent/child2")
  child2 ! "hi, second child"

  parent ! Stop
  for(i <- 1 to 10) parent ! s"parent are you still there $i" // should not be received
  for(i <- 1 to 100) child2 ! s"2nd kid are you still there $i"


  /*
  for stopping a actor
  method #2 - using special messages

  PoisonPill - a special message which triggers Stop
  Kill - logs an error msg of ActorKilledException - actor throws an exception and dies

  both these messages are special and handled separately by actor
  we can't handle them and catch a poisonpill and ignore
   */

  val looseActor = system.actorOf(Props[Child])
  looseActor ! "hello, loose actor"
  looseActor ! PoisonPill
  looseActor ! "are you still there ?"


  val abruptlyTerminatedActor = system.actorOf(Props[Child])
  abruptlyTerminatedActor ! "you are about to be terminated"
  abruptlyTerminatedActor ! Kill
  abruptlyTerminatedActor ! "you have been terminated"

  /*
  Death Watch - a way to get notified when an actor dies
   */
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive : Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)

      case Terminated(ref) => log.info(s"reference that I am watching $ref has been stopped  ")
    }
  }

  val watcher = system.actorOf(Props[Watcher],"watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)
  watchedChild ! PoisonPill



}
