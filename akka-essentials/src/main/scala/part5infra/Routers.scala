package part5infra

import akka.actor._
import akka.routing._
import com.typesafe.config.ConfigFactory

object Routers extends App {
  // route all the messages to its children

  /**
   * # 1 - manual router
   */
  class Master extends Actor {
    // step1 create routees
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // step2 create router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    // step3 receive handler routes the messages
    override def receive: Receive = onMessage(router)

    private def onMessage(router: Router): Receive = {
      // step 4 handle the termination/lifecycle/unexpected exception of the routees
      case Terminated(ref) =>
        context.become(onMessage(router.removeRoutee(ref)))
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        context.become(onMessage(router.addRoutee(newSlave)))
      case message =>
        // When you do that, the slave actors can directly reply to the sender without involving the Master
        router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])
  for (i <- 1 to 10) {
    master ! s"[$i] Hello from the world!"
  }

  /**
   * Method #2 - a router actor with its own children
   * POOL router
   * This actor creates 5 child under its pool
   */
  // Two option, 2.1 programmatically (in code)
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  for (i <- 1 to 10) {
    master ! s"$i Hello from the world"
  }

  // 2.2 from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  println("=" * 50)
  for (i <- 1 to 10) {
    poolMaster2 ! s"$i Hello from the world"
  }

  /**
   * Method #3 - routers with actors created elsewhere
   * GROUP router
   */
  // .. in another part of my application
  val slaveList = for (i <- 1 to 5) yield system.actorOf(Props[Slave], s"slave_$i")

  // 3.1 need their paths
  val slavePaths = slaveList.map(_.path.toString)

  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  for (i <- 1 to 10) {
    groupMaster ! s"$i Hello from the world"
  }

  // 3.2 from configuration
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for (i <- 1 to 10) {
    groupMaster2 ! s"$i Hello from the world"
  }

  /**
   * Special messages
   */
  groupMaster2 ! Broadcast("hello, everyone")

  // PoisonPill and Kill are not routed
  // Add Routee, Remove, Get handled only by the routing actor
}
