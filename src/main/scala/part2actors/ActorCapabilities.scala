package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App{

  class SimpleActor extends Actor {

    override def receive: Receive = {
      case "Deposit" => bankAccountActor ! Deposit(100)
      case "Withdraw" => bankAccountActor ! Withdraw(100)
      case "Statement" => bankAccountActor ! "Statement"
      case "Hi!" => context.sender() ! "Hello, there!"
      case message:String => println(s"[${context.self}] I have received $message from ${context.sender()}")
      case number: Int => println(s"[${self}] I have received a NUMBER : $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received something special : $contents")
      case SendMessageToYourSelf(content) => {
        println("[simple actor]Sending message to oneself")
        self ! content
      }
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // instead of ref ! content (this keeps the original sender)
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  // 1- messages can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  simpleActor ! 42

  // in practice use case classes and case objects
  case class SpecialMessage(content : String)
  simpleActor ! SpecialMessage("some special content")

  // 2. actors have information about their context and about themselves
  // they have a context attribute which has all the info
  // context.self - reference to self, context.system - the actor system
  // so we can context.self to send messages to ourself
  // context.self == `this` in OOP
  // we also have self member which is same as context.self

  case class SendMessageToYourSelf(content : String)
  simpleActor ! SendMessageToYourSelf("I am an actor and I am proud of it")

  // 3 - actors can REPLY to messages

  val alice =  system.actorOf(Props[SimpleActor], "alice")
  val bob =  system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)


  // 4 - dead letters
  alice ! "Hi!" // reply to "me", but sender would be null so it goes to dead letter

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a mesasge with the ORIGINAL sender

  case class WirelessPhoneMessage(content: String, ref:ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob) // noSender (the original sender)
  /*
  Alice received from us, now when alice sends to bob sender is shown as alice for bob
  but original sender are us
   */

  /*
  Exercises
  1. A Counter actor
      - Increment
      - Decrement
      - print the internal counter value to console

  2. A bank account as an actor
      receives
      - Deposit an amount
      - Withdraw an amount
      - Statement

      replies with
      - Success/Failure of each operation

      Logic for bank operations
      interact with some other kind of actor

   */

  class CounterActor extends Actor {
    private var counter = 0
    override def receive: Receive = {
      case "inc" => counter +=1
      case "dec" => counter -=1
      case "show" => println(s"[CounterActor] Counter: $counter")
      case _ => println("[CounterActor] Invalid message")
    }
  }

  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  counterActor ! "inc"
  counterActor ! "show"
  counterActor ! "dec"
  counterActor ! "show"


  case class Deposit(amount: Float)
  case class Withdraw(amount: Float)

  class BankAccountActor extends Actor {
    val fundsMap =  scala.collection.mutable.Map[ActorRef, Float]()
    val transactionMap   = scala.collection.mutable.Map[ActorRef, List[String]]()

    override def receive: Receive = {
      case Deposit(amount) => {
        if(!fundsMap.contains(context.sender())) fundsMap.put(context.sender, 0)
        if(!transactionMap.contains(context.sender())) transactionMap.put(context.sender, List())

        fundsMap.put(context.sender, fundsMap.get(context.sender()).get + amount)

//        println(s"${amount} deposited to acc. Total balance : ${fundsMap.get(context.sender()).get}")
        val list: List[String] = transactionMap.get(context.sender).get :+ s"$amount deposited. Balance ${fundsMap.get(context.sender()).get}"
        transactionMap.put(context.sender(), list)
      }
      case Withdraw(amount) => {
        if(!fundsMap.contains(context.sender())) fundsMap.put(context.sender, 0)
        if(!transactionMap.contains(context.sender())) transactionMap.put(context.sender, List())

        if (amount <= fundsMap.get(context.sender()).get) {
          fundsMap.put(context.sender, fundsMap.get(context.sender()).get - amount)
//          println(s"${amount} withdrawn from acc. Total balance : ${fundsMap.get(context.sender()).get}")
          val list: List[String] = transactionMap.get(context.sender).get :+ s"$amount withdrawn. Balance ${fundsMap.get(context.sender()).get}"
          transactionMap.put(context.sender(), list)
        }else{
          println("Insufficient balance")
        }
      }
      case "Statement" => {
        val list = transactionMap.get(context.sender()).get
        println(list)
        println( "Length :  " + transactionMap.get(context.sender()).size)
        transactionMap.get(context.sender).get.foreach(println(_))
      }
      case _ => println("Unknown operation")
    }
  }

  val bankAccountActor = system.actorOf(Props[BankAccountActor], "bankAccountActor")

  alice ! "Deposit"
  alice ! "Withdraw"
  bob ! "Withdraw"
  alice ! "Statement"
  bob ! "Statement"

}
