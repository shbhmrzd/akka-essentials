package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {
  // part1 - actor systems
  /*
  Actor system is a heavy weight data structure which controls a number of threads under the hood
  and allocates to running actors
   */
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // recommended to have one actorSystem unless there is reason to have more

  // part2 - create actors
  // they are like humans talking to each other
  // word count actor
  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behaviour
    def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I cannot understand ${msg.toString}")
    }
  }

  // part3 - instantiate an actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")

  // akka returns an ActorRef and not actor object, so we cant poke around the actor
  // thus saving encapsulation in a multi threaded environment

  // part4 - communicate!
  wordCounter ! "I am learning Akka and it's pretty damn cool!"
  //    wordCounter.!("I am learning Akka and it's pretty damn cool!")
  // asynchronous!

  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")
  anotherWordCounter ! "A different message"

  /*
  Akka sends messages to actors asynchronously
  Actors are fully encapsulated so we cannot poke them for data, can access their methods and also cannot
  create an instance of the actor
  so
  new WordCounter -  gives error, we can't do it
   */

  // How to instantiate actor with constructor arguments
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi my name is $name")
      case _ =>
    }
  }

  // this is legal, we cannot use new Person() outside but inside actorOf works
  // but this is still not best practice
  val personActor = actorSystem.actorOf(Props(new Person("Bob")))
  personActor ! "hi"

  /*
  THE BEST PRACTICE
  is to create a companion object which has a function calling the constructor of the class
  and returns Prop(Person)
   */
  object Person{
    def props(name: String) = {
      Props(new Person(name))
    }
  }

  val anotherPerson = actorSystem.actorOf(Person.props("Ramesh"))
  anotherPerson ! "hi"

}
