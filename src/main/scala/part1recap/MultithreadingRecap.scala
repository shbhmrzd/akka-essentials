package part1recap

import scala.concurrent.Future
import scala.util.{Success, Failure}

object MultithreadingRecap extends App{

  // creating threads on the jvm
  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("I am running in parallel")
  })

  val aThreadss =  new Thread(() => println("Syntactic sugar way of definfing thread"))

  aThread.start()
  aThreadss.start()
  // wait for thread to finish
  aThreadss.join()
  aThread.join()

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadBye = new Thread(() => (1 to 1000).foreach(_ => println("goodbye")))

  threadHello.start()
  threadBye.start()

  // this is unpredictable, as different runs produce different results

  class BankAccount(@volatile private var amount: Int) {
    override def toString : String = s"amount : $amount"

    def withdraw(money: Int) = this.amount -= money

    def safeWithdraw(money: Int) = this.synchronized {
      this.amount -= money
    }
  }

  /*
  BA(10000)

  t1 -> withdraw 1000
  t2 -> withdraw 2000

  t1 -> this.amount = this.amount - ... // Prempted by the os
  t2 -> this.amount = this.amount - 2000 = 8000
  t1 -> -1000 = 9000

  => result = 9000

  These subtractions are not unit operations
  it comprises of fetching info, updating it and writing it back to memory

  if the thread operation is stopped at fetch and some other flow updates the value
  the subsequent update by the first thread would work on an older value and gives incorrect result

  this.amount = this.amount - 1000 is not ATOMIC (thread safe)


  either make amount : a volatile
  or synchronize the code block

  USING SYNCHRONIZED in a bigger application becomes exhausting
   */

  // inter-thread communication on the JVM
  // wait - notify mechanism

  // Scala Futures
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    // long computation -  executes on a different thread
    42
  }

  // callbacks
  future.onComplete{
    case Success(42) => println("Found the meaning of life")
    case Failure(_) => println("something happened with the meaning of life")
  }

  val aProcessedFuture = future.map(_ + 1) // Future with 43

  val aFlatFuture = future.flatMap({
    value => Future(value + 2)
  }) // Future with 44
  /*
  Syntactic sugar
  val aFlatFuture = future.flatMap{
  value => Future(value + 2)
  }

  we skip the outer paranthesis
   */

  val filteredFuture = future.filter(_ % 2 == 0)
  // returned future is identical if value inside future is even
  // else NoSuchElementException

  //for comprehensions
  val aNonSenseFuture = for {
    meaningOfLife <- future
    filteredMeaning <- filteredFuture
  }yield meaningOfLife + filteredMeaning


  // andThen, recover/recoverWith

  // Promises
}
