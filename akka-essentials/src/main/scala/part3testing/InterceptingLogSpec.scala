package part3testing

import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps

class InterceptingLogSpec
  extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogSpec._
  val item = "Rock the jvm Akka course"
  val creditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "000-1234-1234-1234-1234"

  "A checkout flow" should {
    "correctly log the dispatch of an order" in {
//      log.info(s"Order: $orderId for item: $item has been dispatched.") must be exactly the same with PATTERN
      EventFilter.info(pattern = s"Order: [0-9]+ for item: $item has been dispatched.", occurrences = 1) intercept {
        // our test code here
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, creditCard)
      }
    }

    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }
  }
}

object InterceptingLogSpec {
  case class Checkout(item: String, creditCard: String)
  case class AuthorizeCard(creditCard: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  // Implement a checkout flow of an online shop
  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    // You can view a Receive function as a kind of State
    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied => throw new RuntimeException("I can't handle this anymore")
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if (card.startsWith("0")) sender() ! PaymentDenied
        else {
//          Thread.sleep(4000) // This will cause timeout
          sender() ! PaymentAccepted
        }
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    var orderId = 43

    override def receive: Receive = {
      case DispatchOrder(item: String) =>
        orderId += 1
        log.info(s"Order: $orderId for item: $item has been dispatched.")
        sender() ! OrderConfirmed
    }
  }
}