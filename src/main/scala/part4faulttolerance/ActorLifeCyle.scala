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

}
