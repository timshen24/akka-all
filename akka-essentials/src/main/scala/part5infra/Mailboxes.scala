package part5infra

import akka.actor._
import akka.dispatch._
import com.typesafe.config._

/**
 * Reorder messages before they are handled by the actor and dispatcher
 */
object Mailboxes extends App {
  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * Interesting case #1 - custom priority mailbox
   * P0 -> most important
   * P1 -> second most important
   * P2
   * P3
   */
  // step 1 - mailbox definition, SupportTicketPriorityMailbox is in application.conf
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(PriorityGenerator {
    case message: String if message.startsWith("[P0]") => 0
    case message: String if message.startsWith("[P1]") => 1
    case message: String if message.startsWith("[P2]") => 2
    case message: String if message.startsWith("[P3]") => 3
    case _ => 4
  }) {

  }

  // step 2 - make it known in the config
  // step 3 - attach the dispatcher to an actor
  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"/*in application.conf*/))

  // step 4 - it will receive the messages by priority, even PoisonPill will be postponed
  supportTicketLogger ! PoisonPill
  supportTicketLogger ! "[P3] this thing would be nice to have"
  supportTicketLogger ! "[P0] this needs to be solved now"
  supportTicketLogger ! "[P1] do this when you have time"

  // question, after which time can I send another message and be prioritized accordingly?
  // Sadly, neither can you see nor can you configure the time

  /**
   * Interested case # 2 - control-aware mailbox
   * we'll use UnboundedControlAwareMailbox
   */
  // step 1 - mark important messages as control messages instead of [P0], [P1], ...
  case object ManagementTicket extends ControlMessage

  /**
   * step 2 - configure who gets the mailbox
   * - make the actor attach to mailbox
   */
  // method # 1
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("my-control-mailbox"/*in application.conf*/))
  controlAwareActor ! "[P0] this needs to be solved now"
  controlAwareActor ! "[P1] do this when you have time"
  controlAwareActor ! ManagementTicket

  // method # 2 - using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor"/*in application.conf*/)
  altControlAwareActor ! "[P0] this needs to be solved now"
  altControlAwareActor ! "[P1] do this when you have time"
  altControlAwareActor ! ManagementTicket

}
