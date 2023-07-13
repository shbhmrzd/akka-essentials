package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

object ChildActors extends App {

  // Actors can crate other actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor {
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} Creating child $name")
        // create a new actor right HERE
      val childRef = context.actorOf(Props[Child], name)
      context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => if(childRef != null) childRef forward  message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got : $message")
    }
  }

  import Parent._
  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("child")
  parent ! TellChild("Hey Kid!")

  // actor hierarchies
  // parent -> child -> grandChild
  //        -> child2 ->

  /*
  Guardian actors (top level actors)
  Every akka actor system has 3 guardian actors
  1. /system = system guardian
  2. /user = user-level guardian (for every actor created using system.actorOf, is owned by /user )
          ex. akka://ParentChildDemo/user/parent - path for the parent actor
           akka://ParentChildDemo/user/parent/child - path for child actor
  3. / = the root guardian (both system and user belong to this, manages user and system actors)
            and user actor manages every actor we create
   */


  /*
  Actor Selection
   */
  val childSelection = system.actorSelection("/user/parent/child")
  // childSelection : ActorSelection which is a wrapper for a potential actor
  // we locate an actor using this
  // if the path is not valid, then we get [INFO] message was sent to deadletter as it could not find an actor
  childSelection ! "I found you"

  /*
  Danger!
  NEVER PASS MUTABLE ACTOR STATE, OR THE `THIS` REFERENCE, TO CHILD ACTORS.
  NEVER IN YOUR LIFE.
   */

  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)

    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._
    var amount = 0
    def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // !!
      /*
      MISTAKE 2
      The argument above `this` compounds the problem, by sending actual object reference
      we are exposing and enable child actors to method calls from other actors
      this also opens up to concurrency problems
      thus breaking the actor principles
       */
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard {

    case class AttachToAccount(bankAccount: NaiveBankAccount) // this is of type Actor and not actorref !!!
    /*
    MISTAKE 1
    Because instead of ActorRef this message contains of actual actor jvm object
    normally we would do
    case class AttachToAccount(bankAccountRef: ActorRef)
     */
    case object CheckStatus
  }
  class CreditCard extends Actor {

    def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed")
        // benign
        account.withdraw(1) // because I can
        /*
        MISTAKE 3
        Closing over - Never close over mutable state of the `this` reference
        Scala doesnt check on compile time so we have to take care
         */
    }

    def receive : Receive = {
      case AttachToAccount(account) => context.become(attachedTo(account))
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)
  // look for child actor of bank account which would be credict actor
  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus

  // WRONG !!!!!
}
