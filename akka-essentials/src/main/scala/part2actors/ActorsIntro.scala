package part2actors

import akka.actor._

object ActorsIntro extends App {
  // part 1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part 2 - create actors
  // word count actor
  class WordCountActor extends Actor {
    var totalWords = 0

    // behaviour
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I can't understand ${msg.toString}")
    }
  }

  // part3 - instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // part4 - communicating with Akka
  wordCounter ! "I am learning Akka and it's pretty damn cool!" // tell method
  anotherWordCounter ! "A different message"
  // asynchronous!

  // part5 - pass in constructor parameters, which is discouraged
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val personDiscouraged = actorSystem.actorOf(Props(new Person("Bob")))
  personDiscouraged ! "hi"

  object Person {
    def props(name: String): Props = Props(new Person(name))
  }

  val person = actorSystem.actorOf(Person.props("Bob"))
  person ! "hi"
}
