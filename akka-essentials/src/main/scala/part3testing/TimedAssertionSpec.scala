package part3testing

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionSpec
  extends TestKit(ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionSpec._
  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with the meaning of life in a timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence"

        // within 2 seconds, I want to have 10 messages, with no gap between any of 2 messages exceeds 500 millis
        val results: Seq[Int] = receiveWhile[Int](max=2 seconds, idle = Duration.Zero/*500 millis*/, messages = 10) {
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
      }
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42)) // NOTICE: the probe here has its own timeout, not from the `within` block, but set by the `specialTimedAssertionsConfig`
      }

    }
  }
}

object TimedAssertionSpec {
  case class WorkResult(result: Int)
  // testing scenario
  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)

      case "workSequence" =>
        val r = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}