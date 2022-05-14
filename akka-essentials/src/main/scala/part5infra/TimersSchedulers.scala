package part5infra

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulers extends App {
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulerTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  import system.dispatcher // Exactly the same thing
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder"
  }

  /**
   * 2. Scheduling a repeated message
   */
  val routine: Cancellable = system.scheduler.scheduleWithFixedDelay(1 second, 2 seconds) {
    () => simpleActor ! "heart!"
  }

  // Cancel this one
  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }

  /**
   * Exercise: implement a self-closing actor
   *
   * - if the actor receives a message(anything), you have 1 second to send it another message
   * - if the timed window expires, the actor will stop itself
   * - if you send another message, the time window is reset
   */
}
