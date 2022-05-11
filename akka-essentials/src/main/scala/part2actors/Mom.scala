package part2actors

import akka.actor._
import part2actors.ChangingActorBehaviour.StatelessFussyKid._

class Mom extends Actor {
  import Mom._

  override def receive: Receive = {
    case MomStart(kidRef) =>
      // test our application
      kidRef ! Food(VEGETABLES)
      kidRef ! Ask("do you want to play?")
    case KidAccept => println("Yay, my kid is happy")
    case KidReject => println("My kid is sad, but he is healthy")
  }
}

object Mom {
  case class MomStart(kidRef: ActorRef)
  case class Food(food: String)
  case class Ask(message: String) // Do you want to play?
  val VEGETABLES = "veggies"
  val CHOCOLATE = "chocolate"
}