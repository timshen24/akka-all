package part4faulttolerance

import akka.actor._

object ActorLifecycle extends App {

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {
    // new inherited method, effective to Child actor as well
    override def preStart(): Unit = log.info("I am starting")
    // new inherited method, effective to Child actor as well
    override def postStop(): Unit = log.info("I have stopped")

    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
  val parent = system.actorOf(Props[LifecycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill

  /**
   * restart
   */
  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor {
    val child: ActorRef = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check

    }
  }

  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = {
      log.info("supervised child started")
    }

    override def postStop(): Unit = {
      log.info("supervised child stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised actor restarting because of ${reason.getMessage}")
    }


    override def postRestart(reason: Throwable): Unit = {
      log.info("supervised actor restarted")
    }

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

  // Default supervision strategy: If an actor throw an Exception while processing a message, this message which cause the exception is removed from the queue and not back to the mailbox again. The actor is restarted which means the mailbox is untouched!!

}
