package part4faulttolerance

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import SupervisionSpec._

  "A supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because I am learning and thinking in a whole new way" // Resume
      child ! Report
      expectMsg(3) // throw a Runtime Exception which will not change its state
    }

    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report // Restarted
      expectMsg(0) // Because when restarted, the internal state of actor is cleared.
    }

    "terminate its child in case of major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! "akka is nice"
      val terminatedMessage = expectMsgType[Terminated] // Stop strategy
      assert(terminatedMessage.actor == child)
    }

    "escalate an errror when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 43 // Escalate strategy
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalate its failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      child ! expectMsg(3)

      child ! 45 // Because we write the preRestart of NoDeathOnRestartSupervisor, it's child will not be killed, only restarted, hence clearing its original state
      child ! Report
      child ! expectMsg(0)
    }
  }

  "An all for one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]

      secondChild ! "Testing supervision"
      secondChild ! Report
      expectMsg(2)

      /**
       * Wait for the NPE to be caught from the child ActorRef
       */
      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      Thread.sleep(500)
      secondChild ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {
  case object Report

  class Supervisor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate // Escalate will by default restart and kill itself and its children
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {}
  }

  class AllForOneSupervisor extends Supervisor {
    /**
     * In case one child fails with one of the strategy, all children fail with the same strategy
     */
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate // Escalate will by default restart and kill itself and its children
    }
  }

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case _ => throw new Exception("can only receive strings")
    }
  }
}
