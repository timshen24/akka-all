package part2actors

import akka.actor._

object ActorCapabilities extends App {
  class SimplerActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => /*context.*/sender() ! "Hello! there!" // 4 replying to a message. The sender here refers to Bob
      case message: String => println(s"[${context.self.path}] I have received $message")
      case number: Int => println(s"[${self}] I have received a NUMBER: $number")
      case SpecialMessage(contents) => println(s"[simple actor] I have received something SPECIAL $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => (ref ! "Hi!")(self) // 9 In this case, Alice is being passed as the sender. But if I do forwarding, I can keep the original sender, which is noSender

      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // I keep the original sender of the WirelessPhoneMessage
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimplerActor], "simpleActor")
  simpleActor ! "hello, actor" // 5 exclamation mark means send message to the caller

  // 1 - messages can be any type
  // a) messages must be immutable
  // b) messages must be SERIALIZABLE
  // In practice, use case classes and case objects
  simpleActor ! 42 // 6 Who is the sender?

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their content and about themselves
  // context.self === `this` in OOP
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimplerActor], "alice")
  val bob: ActorRef = system.actorOf(Props[SimplerActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  alice ! "Hi!" // 7 the sender is dead letters, which is the garbage pool of messages

  // 8 Forwarding messages = sending a message with the ORIGINAL sender
  // Daniel -> Alice -> Bob

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi!", bob) // 10 noSender here
}
