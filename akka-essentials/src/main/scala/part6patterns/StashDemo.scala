package part6patterns

import akka.actor._

object StashDemo extends App {
  /**
   * ResourceActor
   *   - open => it can receive read/write requests to the resources
   *   - otherwise it will postpone all read/write requests until the state is open
   *   ResourceActor is closed
   *     - open => switch to the open state
   *     - Read, Write messages are postponed
   *   ResourceActor is open
   *     - Read, Write are handled
   *     - Close => switch to the closed state
   *   Assume:
   *   [Open, Read, Read, Write]
   *   - switch to open state
   *   - read the data
   *   - read the data again
   *   - write the data
   *
   *   [Read, Open, Write]
   *   - stash Read
   *     stash: [Read]
   *   - open => switch to the open state
   *     Mailbox : [Read, Write]
   *   - read and write are handled
   *
   */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step1 - mix-in the Stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    /**
     * While unstashAll, ALL stashed messages will be dealt right away
     * @return
     */
    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // step 3 - unstashall() when you switch the message handler
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        // step 2 - stash away what you can't handle
        stash()
    }

    def open: Receive = {
      case Read =>
        // do some actual computation
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I am writing $data")
        innerData = data
      case Close =>
        log.info("closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }

    override def receive: Receive = closed
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Write("I love stash")
  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read
}
