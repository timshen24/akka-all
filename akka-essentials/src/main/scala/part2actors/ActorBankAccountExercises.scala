package part2actors

import akka.actor._

object ActorBankAccountExercises extends App {
  import BankAccount._
  import Person._
  /**
   * 2. a BankAccount actor
   *    receives
   *    - Deposit an amount
   *    - Withdraw an amount
   *    - Statement
   *    replies with
   *    - Success/Failure
   *
   *    interact with some other kind of actor to test effect and interpret the BankAccount actor's internal Success/Failure status.
   **/
  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"Successfully deposited $amount")
        }
      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Invalid withdraw amount")
        else if (amount > funds) sender() ! TransactionFailure("Insufficient funds")
        else {
          funds -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew $amount")
        }
      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  val system: ActorSystem = ActorSystem("ActorBankAccountExercises")

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account: ActorRef = system.actorOf(Props[BankAccount], "bankAccount")
  val person: ActorRef = system.actorOf(Props[Person], "billionaire")

  /**
   * Must not do this, because the sender here is deadLetter. There is no way for it to receive any message returned from the BankAccount.
   * */
//  account ! Deposit(10000)
  //  account ! Withdraw(90000)
  //  account ! Withdraw(500)
  //  account ! Statement

  person ! LiveTheLife(account)
  person ! Statement
}
