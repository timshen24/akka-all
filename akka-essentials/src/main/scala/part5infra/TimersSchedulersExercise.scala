package part5infra

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulersExercise extends App {
  /**
   * Exercise: implement a self-closing actor
   *
   * - if the actor receives a message(anything), you have 1 second to send it another message
   * - if the timed window expires, the actor will stop itself
   * - if you send another message, the time window is reset
   */
  val system = ActorSystem("SchedulerTimerExercise")
  import system.dispatcher

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule: Cancellable = createTimeoutWindow()
    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message, staying alive")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis) {
    selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce(2 seconds) {
    system.log.info("sending pong to the self-closed actor")
    selfClosingActor ! "pong"
  }

  /**
   * Time - for actors to send message to itself
   */
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop

  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerHeartbeatActor = system.actorOf(Props[TimerBasedHeartBeatActor], "timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }
}
