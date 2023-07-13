package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {
  class SimpleActorWithExplicitLogger extends Actor {
    // #1 - explicit logging
    val logger = Logging(context.system, this)
    override def receive: Receive = {
      /*
      1 - Debug - most verbose
      2 - Info - benign messages
      3 - Warn - ex - messages sent to dead letters, something that might be a concern
      4 - Error - things which could cause issue
       */
      case message => logger.info(message.toString) // Log it
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])

  actor ! "Logging a simple message"

  // #2 - ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a,b) => log.info("two things  {} and {}", a, b)
      case message => log.info(message.toString)
    }
  }

  Thread.sleep(100)
  val simplerActor = system.actorOf(Props[ActorWithLogging])
  simplerActor ! "Logging a simple message"

  simplerActor ! ("conviva", "streaming")

}
