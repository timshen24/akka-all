package part2actors

import akka.actor._

/**
 * This approach use a var to represent state, which is not recommended. We like more concise code and immutable val.
 */
object ChangingActorBehaviourDiscouraged extends App {
  import FussyKid._
  import MomActor._

  class FussyKid extends Actor {
    var state: String = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLES) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class MomActor extends Actor {
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our application
        kidRef ! Food(VEGETABLES)
        kidRef ! Ask("do you want to play?")
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad, but he is healthy")
    }
  }

  object MomActor {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // Do you want to play?
    val VEGETABLES = "veggies"
    val CHOCOLATE = "chocolate"
  }

  val system = ActorSystem("ChangingActorBehaviourDemo")
  val fussyKid = system.actorOf(Props[FussyKid])
  val mom = system.actorOf(Props[MomActor])

  mom ! MomStart(fussyKid)
}
