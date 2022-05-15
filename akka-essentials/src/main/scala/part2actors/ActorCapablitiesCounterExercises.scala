package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapablitiesCounterExercises extends App{
  import Counter._
  /**
   * 1. a Counter actor
   *    - Increment
   *    - Decrement
   *    - Print
   **/

  // DOMAIN of the counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => print(s"[count] My current count is $count")
    }
  }

  val system = ActorSystem("ExerciseDemo")
  private val counter: ActorRef = system.actorOf(Props[Counter], "My Counter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)

  counter ! Print
}
