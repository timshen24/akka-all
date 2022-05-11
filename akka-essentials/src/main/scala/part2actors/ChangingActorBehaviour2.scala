package part2actors

import akka.actor._

object ChangingActorBehaviour2 extends App {
  import Mom._
  import StatelessFussyKid._

  class StatelessFussyKid extends Actor {
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, discardOld = false) // push happyReceive to stack
      case Food(CHOCOLATE) => // do nothing
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, discardOld = false)
      case Food(CHOCOLATE) => context.unbecome() // push happyReceive to stack
      case Ask(_) => sender() ! KidReject
    }
  }

  object StatelessFussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class Mom extends Actor {
    import Mom._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our application
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
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

  val system = ActorSystem("ChangingActorBehaviourDemo")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])

  mom ! MomStart(statelessFussyKid)

  /**
   * Mom receives MomStart
   *   kid receives Food(veg) -> kid will change the handler to sadReceive
   *   kid receives Ask(play?) -> kid replies with the sadReceive handler
   * Mom receives KidReject
   */

  /**
   * Food(veg) -> message handler turns to sadReceive
   * Food(chocolate) -> becomes happyReceive
   */

  /**
   * context.become
   * Food(veg) -> stack.push(sadReceive)
   * Food(chocolate) -> stack.push(happyReceive)
   *
   * Stack looks like:
   * 1. happyReceive
   * 2. sadReceive
   * 3. happyReceive
   * 4. override def receive: Receive = ???
   */

  /**
   * new Behaviour
   * Food(veg)
   * Food(veg)
   * Food(chocolate)
   *
   * Stack:
   * 1. sadReceive (this one will be poped)
   * 2. sadReceive
   * 3. happyReceive
   */
}
