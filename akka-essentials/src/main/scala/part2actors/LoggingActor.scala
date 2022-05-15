package part2actors

import akka.actor._
import akka.event.{Logging, LoggingAdapter}

object LoggingActor extends App {
  // # - 1 Explicit Logging
  class SimpleActorWithExplicitLogging extends Actor {
    val logger: LoggingAdapter = Logging(context.system, this)
    override def receive: Receive = {
      case message => logger.info(message.toString) // LOG IT
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogging])

  actor ! "Logging a simple message"

  // #2 - ActLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
      case message => log.info(message.toString) // LOG IT
    }
  }

  val simplerActor = system.actorOf(Props[ActorWithLogging])
  simplerActor ! "Logging a simple message by extending a trait"
  simplerActor ! (42, 65)
}
