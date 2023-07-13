package part1recap

import scala.annotation.tailrec
import scala.util.Try

object GeneralRecap extends  App {

  val aCondition: Boolean = true
  var aVariable = 42
  aVariable += 1

  // expressions
  val aConditionedVal = if (aCondition) 42 else 65

  // code block
  val aCodeBlock = {
    if(aCondition) 74
    56
  }

  print(aCodeBlock)

  // types
  // Unit
  val theUnit = println("Hello Scala!")

  def aFunction(x: Int): Int = x+1

  @tailrec
  def factorial(n:Int, acc:Int): Int = {
    if(n <= 0) acc
    else factorial(n-1, acc*n)
  }

  //OOP
  class Animal
  class Dog extends Animal

  val dog: Animal = new Dog

  trait Carnivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Carnivore {
    override def eat(a: Animal): Unit = println("Crunch crunch")
  }

  // method notations
  val aCroc = new Crocodile

  aCroc.eat(dog)
  aCroc eat dog

  // anonymous class
  // nice way to instantiate classes that extend from abstract type on the spot
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("roar")
  }

  // generics
  abstract class MyList[+A]
  /*
  +A indicates covariance
  a list of dogs is also an extension of list of animals
  as dogs are extension of animals
   */

  // companion objects
  // singleton object
  object MyList

  // case classes
  case class Person(name: String, age: Int)

  // Exceptions
  val aPotentialFailure = try {
    throw new RuntimeException("I am innocent, I swear!") // returns Nothing, doesnt return anything at all not null not nunit, just returns Nothing and crashes the jvm
  }catch{
    case e: Exception => "I caught an exception"
  }finally {
    // side effetcs, happen no matter what
    println("some logs")
  }

  // Functional programming
  // make functions objects
  val incrementer = new Function1[Int, Int]{
    override def apply(v1: Int): Int = v1 + 1
  }
  val incremented = incrementer(42) // 43
  // incrementer.apply(42)
  // incrementer is a function, but under the hood it is an instance of the class

  val anonymousIncrementer = (x:Int) => x + 1
  // Int => Int === Function1[Int,Int]

  // FP is all about working with functions as first-class
  List(1,2,3).map(incrementer)
  // map is a Higher order function as it takes function as a parameter and returns another function as result

  // for comprehensions
  val pairs = for {
    num <- List(1,2,3,4)
    char <- List('a','b','c','d')
  }yield num + "-" + char

  // this translates to
  // List(1,2,3,4).flatMap(num => List('a','b','c','d').map(char => num + "-" + char))

  // Seq, Array, List, Vector, Map, Tuples, Sets

  // "collections"
  // Option and Try
  val anOption = Some(2)
  // wrap with try something which might throw exception
  val aTry = Try {
    throw new RuntimeException()
  }

  // pattern matching
  val unknown = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  // pattern matching is not just switch case
  // it can decompose and bind value
  val bob = Person("Bob", 22)
  val greeting = bob match {
    case Person(n, _) => s"Hi, my name is $n"
    case _ => "I don't know my name"
  }

  // all the patterns
}