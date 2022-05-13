package part3testing

import akka.actor._
import akka.testkit.{CallingThreadDispatcher, TestActor, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.SynchronousTestingSpec.Counter

import scala.concurrent.duration.Duration

// SynchronousTesting == When you send actor a message, basically your are assured the actor received the message
class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  import SynchronousTestingSpec._
  "A counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc // The TestActorRef has the capability that when I sent Inc counter has ALREADY received the message

      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc) // just the same as counter ! Inc for TestActorRef
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // PROBE has ALREADY received the message 0. Because the probe is working on the `CallingThreadDispatcher`
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0

    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}