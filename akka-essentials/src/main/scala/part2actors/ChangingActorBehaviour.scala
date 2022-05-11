package part2actors

import akka.actor._
//import part2actors.ChangingActorBehaviourDiscouraged.Mom

object ChangingActorBehaviour extends App {
  import StatelessFussyKid._
  import Mom._

  class StatelessFussyKid extends Actor {
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive) // change receive handler to sadReceive
      case Food(CHOCOLATE) => // do nothing
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLES) => // do nothing
      case Food(CHOCOLATE) => context.become(happyReceive) // change receive handler to happyReceive
      case Ask(_) => sender() ! KidReject
    }
  }

  object StatelessFussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
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
}
