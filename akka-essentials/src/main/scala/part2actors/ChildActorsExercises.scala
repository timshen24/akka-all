package part2actors

import akka.actor._

object ChildActorsExercises extends App {
  // Distributed Word counting

  /**
   * create WordCounterMaster
   * send Initialize(10) to WordCounterMaster
   * send "Akka is awesome" to WordCounterMaster
   * Then WordCounterMaster will send a WordCountTask("...") to one of its children
   * child replies with a WordCountReply(3) to the master
   * master replies with 3 to the sender
   *
   * requester -> wcm -> wcw
   * requester <- wcm <-
   *
   * round robin logic
   *
   */

  import WordCounterMaster._

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {

    def withChildrenRef(childrenRef: Seq[ActorRef]): Receive = {
      case WordCountTask(id, text) => childrenRef(id % childrenRef.length) forward (id, text)
      case WordCountReply(id, count) => println(s"id: $id replies $count")
    }

    override def receive: Receive = {
      case Initialize(nChildren) =>
        val childrenRef: Seq[ActorRef] = (1 to nChildren).map(i => context.actorOf(Props[WordCounterWorker], s"worker$i"))
        context.become(withChildrenRef(childrenRef))
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {
      case (id: Int, text: String) => {
        println(s"${self.path} received text: $text")
        context.parent ! WordCountReply(id, text.split(" ").length)
      }
    }
  }

  val system = ActorSystem("WordCountSystem")
  val wordCounterMaster = system.actorOf(Props[WordCounterMaster], "master")
  wordCounterMaster ! Initialize(10)
  wordCounterMaster ! WordCountTask(1, "Akka is awesome")
}
