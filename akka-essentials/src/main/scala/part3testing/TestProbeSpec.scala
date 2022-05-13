package part3testing

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterAll

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll
{
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // A testProbe is a special character with some assertion capabilities
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }

    "send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)

      // here we are testing the interaction between the master Actor and the slave Actor
      slave.expectMsg(SlaveWork(workLoadString, testActor))
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3)) // Because testActor receives the Report(3)
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)
      master ! Work(workLoadString)

      // this is used when one needs to reply with multiple messages, such as do aggregation
      slave.receiveWhile() {
        case SlaveWork(`workLoadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec {
  // scenario
  /**
   * word counting actor hierarchy master-slave
   * send some work to the master
   *   - master sends to slave the piece of work
   *   - slave process the work and replies to master
   *   - master aggregates the result
   * master sends the total count to the original requester
   */
  case class Work(text: String)
  case class SlaveWork(text: String, originalRequest: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Register(slaveRef: ActorRef)
  case object RegistrationAck
  case class Report(totalCount: Int)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef: ActorRef) =>
        sender() ! RegistrationAck
        context.become(online(slaveRef, 0))
      case _ => // ignore
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  // class Slave extends Actor {...}


}
