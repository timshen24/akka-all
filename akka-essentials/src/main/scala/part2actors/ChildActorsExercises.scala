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
    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"[master] initializing...")
        val childrenRef: Seq[ActorRef] = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRef, 0, 0, Map()))
    }

    // 2 A pattern: keeping in track of which reply is associated with which request
    // Especially useful when need to follow a huge bunch of children tasks
    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have received: $text - I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        // 1 problem. How shall I send the result back to the original sender?
        println(s"[master] I have received a reply for task id $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I love Akka", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I received a reply: $count")
    }
  }

  val system = ActorSystem("RoundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"
}
