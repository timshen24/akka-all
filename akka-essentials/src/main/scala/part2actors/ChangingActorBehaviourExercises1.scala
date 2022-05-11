package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviourExercises1 extends App {
  /**
   * 1. rewrite Counter using become/unbecome and no mutable state
   */
  import Counter._
  // DOMAIN of the counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    def counterReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[$currentCount] incrementing by 1")
        context.become(counterReceive(currentCount + 1))
      case Decrement =>
        println(s"[$currentCount] decrementing by 1")
        context.become(counterReceive(currentCount - 1))
      case Print => print(s"[count] My current count is $currentCount")
    }

    override def receive: Receive = counterReceive(0)
  }

  val system = ActorSystem("ExerciseDemo")
  private val counter: ActorRef = system.actorOf(Props[Counter], "MyCounter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)

  counter ! Print
}
