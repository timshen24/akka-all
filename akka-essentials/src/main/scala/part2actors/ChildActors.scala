package part2actors

import akka.actor._
import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

object ChildActors extends App {
  import Parent._
  // Actor can create Actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {

    //    var child: ActorRef = null
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        // create a new actor right here
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message: String => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("hey kid!")

  // actor hierarchies
  // parent -> child -> grandChild
  //        -> child2 -> ...

  /**
   * Guardian actors are top level actors
   * - /system = system guardian
   * - /user = user guardian
   * - / = root guardian
   * */

  /**
   * Actor selection by its path
   */
  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you"

  /**
   * Danger!
   *
   * NEVER pass mutable actor state, or `this` reference, to child actors.
   *
   * NEVER IN YOUR LIFE
   */
  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }

  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // 2. WHY NOT??
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int): Unit = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }

    def withdraw(funds: Int): Unit = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // 1. DANGER! // 4. How to fix? Must use ActorRef instead of the instance of Actual Actor JVM objects. NEVER call methods, only sending messages
    case object CheckStatus
  }

  class CreditCard extends Actor {

    def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed")
        // benign
        account.withdraw(1) // 3. because I can, that's the problem. This behaviour in which an akka state suddenly be changed without the use of messages is EXTREMELY EXTREMELY hard to DEBUG!
        // It will bypass all message logic we defined in `part2actors.ChildActors.NaiveBankAccount.receive`
    }

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachedTo(account))
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)
  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus

  // WRONG!!!!!
}
