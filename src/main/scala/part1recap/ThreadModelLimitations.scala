package part1recap
import scala.concurrent.Future

object ThreadModelLimitations extends App {

  /*
  Daniel's rants
   */

  /**
   * DR #1: OOP encapsulation is only valid in the SINGLE THREADED MODEL
   */
  class BankAccount(private var amount: Int) {
    override def toString: String = ""+amount
    def withdraw(money: Int) = this.synchronized{
      this.amount -= money
    }
    def deposit(money: Int) = this.synchronized{
      this.amount += money
    }
    def getAmount = amount
  }

  val account = new BankAccount(2000)
//  for(_ <- 1 to 1000){
//    new Thread(() => account.withdraw(1)).start()
//  }
//
//  for(_ <- 1 to 1000){
//    new Thread(() => account.deposit(1)).start()
//  }

  println(account)
  println(account.getAmount)

  /*
  When it comes to multithreading, encapsulation can be challenging to maintain due to the concurrent execution of multiple threads.
  Multithreading involves the execution of multiple threads simultaneously, where each thread can access and modify shared data.

  If proper synchronization mechanisms are not implemented, multiple threads can access and modify the encapsulated data concurrently,
  leading to potential conflicts and data corruption. This can break encapsulation because the internal state of an object,
   which should be hidden and controlled by the class, can be modified by multiple threads simultaneously.

   But synchronization, could lead to deadlocks, livelocks
   */

  /****
   * DR 2 Delegating something to a thread is a pain
   *
   * You have a running thread, and you want to pass a runnable to that thread.
   */
  var task: Runnable = null
  val runningThread: Thread =  new Thread(() => {
    while(true){
      while(task == null){
        runningThread.synchronized {
          println("[background] waiting for a task ...")
          runningThread.wait()
        }
      }
      task.synchronized{
        println("[background] I have a task .. ")
        task.run
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if(task == null) task = r
    runningThread.synchronized{
      runningThread.notify()
    }
  }

  runningThread.start()

  Thread.sleep(500)
  delegateToBackgroundThread(() => {
    println(42)
  })
  Thread.sleep(1000)
  delegateToBackgroundThread(() => {
    println("This should run in the background")
  })

  /**
   * DR 3 : tracing and dealing with errors in a multithreaded env is a PAIN
   */
  // 1M numbers in between 10 threads

  import scala.concurrent.ExecutionContext.Implicits.global

  val futures = (0 to 9)
    .map(i => 100000 * i until 100000*(i+1) ) // 0 - 99999, 100000 - 199999, 200000 - 299999 etc
    .map(range => Future {
      if(range.contains(546735)) throw new RuntimeException("invalid number")
      range.sum
    })

  val sumFuture =  Future.reduceLeft(futures)(_ + _)
  sumFuture.onComplete(println)


}


/*
Thread model limitations

OOP is not encapsulated due to race conditions, as multiple threads can modify at the same time

Locks to the rescue ?
- deadlocks, livelocks, headaches
- a massive pain in distributed environments

Delegating tasks
- hard, error prone
- never feels "first class" although often needed
- should never be done in blocking fashion

Dealing with error
- a monumental task in even small systems
-
 */