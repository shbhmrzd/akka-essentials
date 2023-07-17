package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCyle extends App {

  object StartChild
  class LifecycleActor extends Actor with ActorLogging {

    // called before the actor instance has chance to process any event
    override def preStart(): Unit = log.info(s"$self I am starting")

    override def postStop(): Unit = log.info(s" $self I have stopped")
    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
  val parent = system.actorOf(Props[LifecycleActor],"parent")
  parent ! StartChild

  parent ! PoisonPill
  /*
  Above shows that parent preStart is called before child
  but child postStop is called when parent is sent message poisonpill
  because recursively the children are killed first and then the parent
   */


  /*
  Restart
   */

  object Fail
  object FailChild

  class Parent extends Actor {
    private val child = context.actorOf(Props[Child], "supervisedChild")
    def receive : Receive = {
      case FailChild => child ! Fail
    }
  }
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info(s"$self supervised child started")

    override def postStop(): Unit = log.info(s"$self supervised child stopped")

    // called by the old actor instance before it is swapped
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"$self supervised actor restarting because of ${reason.getMessage}")

    // called by the new actor instance after it is swapped
    override def postRestart(reason: Throwable): Unit = log.info(s"$self supervised actor restarted ")
    override def receive: Receive = {
      case Fail =>
        log.warning(s" $self child will fail now")
        throw new RuntimeException(s" $self I failed")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild

}
