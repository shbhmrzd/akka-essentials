package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /*
  Different ways to configure akka
  1. inline configuration
   */

  val configString =
    """
      |akka {
      | loglevel = ERROR
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", config)

  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  /*
  2 - config file (most practiced)
  application.conf which stays in src/main/resources
  Akka system looks at this file by default
   */

  val defaultConfigSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigSystem.actorOf(Props[SimpleLoggingActor])

  defaultConfigActor ! "Remember me"


  /*
  3 - separate config in the same file
  # created a new namespace in application.conf and created akka properties again
   */

  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor =  specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "A special message"


  /*
  4 - A separate config in another file
  src/main/resources/secretFolder/secretConfiguration.conf
   */
  val secretConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"separate config log level : ${specialConfig.getString("akka.loglevel")}")

  /*
  5 - different file formats
  JSON, Properties
   */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"prop config : ${propConfig.getString("my.simpleProperty")}")
  println(s"prop config : ${propConfig.getString("akka.loglevel")}")
}
