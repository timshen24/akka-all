package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCounterExercises extends App{
  /**
   * 1. a Counter actor
   *    - Increment
   *    - Decrement
   *    - Print
   **/
  case class Counter(cnt: Int) {
    def increment: Counter = this.copy(cnt + 1)
    def decrement: Counter = this.copy(cnt - 1)

    def print(): Unit = println(s"count = $cnt")
  }

  var counter = Counter(0)

  class CounterActor extends Actor {
    override def receive: Receive = {
      case "Increment" => counter = counter.increment
      case "Decrement" => counter = counter.decrement
      case "Print" => counter.print()
    }
  }

  val system = ActorSystem("ExerciseDemo")
  private val counterActor: ActorRef = system.actorOf(Props[CounterActor], "CounterActor")
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Increment"
  counterActor ! "Decrement"
  counterActor ! "Decrement"
  counterActor ! "Print"
}
