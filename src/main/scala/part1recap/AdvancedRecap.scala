package part1recap

import scala.concurrent.Future

object AdvancedRecap extends App{

  // partial functions
  // functions which operate on a subset of given input domain
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }
  // for any value apart from 1,2 and 5 it throws exception
  // partial functions are then mix of int to int function plus the pattern matching
  val pf = (x:Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  // valid definition
  val function : (Int => Int) = partialFunction

  // since partial functions are extension of normal function
  // collections may operate on partial functions as well
  val modifiedList = List(1,2,3).map({
    case 1 => 42
    case _ => 0
  })
  /*
  syntactic sugar we can drop the paranthesis after map
  val modifiedList = List(1,2,3).map{
    case 1 => 42
    case _ => 0
  }
   */

  // lifting
  val lifted = partialFunction.lift
  // converts it from partial function to total function
  // Int => Option[Int]
  lifted(2) // Some(65)
  lifted(3) // None, but now it doesn't throw match error unlink before

  // partial function chaining
  // orElse
  val pfChain = partialFunction.orElse[Int,Int]{
    case 60 => 9000
  }

  pfChain(5) // 999 per partialFunction
  pfChain(60) // 9000
  //  pfChain(457) // throw a matchError

  // type aliases
  type ReceivedFunction = PartialFunction[Any, Unit]

  def receive: ReceivedFunction = {
    case 1 => println("Hello")
    case _ => println("Confused ..")
  }

  // implicits
  implicit val timeoutalpha = 3000

  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout")) // extra parameter list omitted

  // implicit conversions
  // 1) implicit defs

  case class Person(name:String){
    def greet = s"Hi my name is $name"
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)
  "Peter".greet
  // fromStringToPerson("Peter").greet - automatically done by the compiler
  // the compiler knows there is greet on a person, and there is an implicit function
  // from string to Person
  // so now we are able to call the function directly on string

  // 2) implicit classes
  implicit class Dog(name: String){
    def bark = println("bark!")
  }
  "Lassie".bark
  // new Dog("Lassie").bark - automatically done by the compiler
  // whatever type is taken as an argument by either the function or the class
  // that classes behaviour is now extended

  // organize implicits
  // decide how compiler fetches the implicit value
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  // takes the implicit definition of the Ordering[Int] using local scope
  List(1,2,3).sorted // returns List(3,2,1)

  // imported scope
  // compiler warns no implicit found for scala.concurrent.ExecutionContext
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    println("Hello future")
  }

  /*
  Order which compiler uses
  1. local scope
  2. imported scope
  3./ companion objects of the type included in the call
   */


  // companion objects of the types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("Bob"), Person("Rachel"), Person("Alice")).sorted
  // returns List(Person("Alice"), Person("Bob"), Person("Rachel"))

}
