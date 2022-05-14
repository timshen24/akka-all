package part4faulttolerance

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern._

import scala.concurrent.duration._
import scala.io.Source
import java.io.File
import scala.language.postfixOps

object BackOffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {

    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.warning("Persistent actor has stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("Persistent actor restarting")
    }

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null) {
          /**
           * Notice the file path here!!
           */
          dataSource = Source.fromFile(new File("akka-essentials/src/main/resources/testfiles/important_data.txt"))
        }
        log.info(s"I've just read some important data: ${dataSource.getLines().toList}")

    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//  simpleActor ! ReadFile

  val simpleSupervisorProbes: Props = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds, // then 6s, 12s, 24s,...,
      30 seconds,
      0.2
    )
  )

  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProbes, "simpleSupervisor") // 创建时，子 child 也一起创建了
//  simpleBackoffSupervisor ! ReadFile // simpleSupervisor will forward to its child

  /**
   * simpleSupervisor
   *  - first creates a child called simpleBackoffActor (props of type FileBasedPersistentActor)
   *  - the simpleSupervisor can receive any message and forward them to its child
   *  - supervision strategy is the default one (namely, restarting on everything)
   *    - when it fails:
   *      - first attempt after 3 seconds
   *      - next attempt is 2x the previous attempt, i.e. 3s then 6s, 12s, 24s,...,
   *      - 0.2 is the distraction factor, to prevent huge amount of child restart at the same time
   */


  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop (
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(OneForOneStrategy() {
      case _ => Stop
    })
  )

  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  stopSupervisor ! ReadFile

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("akka-essentials/src/main/resources/testfiles/important_data.txt"))

    }
  }

  val eagerFBPActor = system.actorOf(Props[EagerFBPActor])
  // IF an actor tries to throw an ActorIntializationException => means STOP

  /**
   * The backoff supervisor will kick on the stopped, starting it again
   */
  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /**
   * eagerSupervisor
   *   - child eagerActor
   *     - will die on start with ActorIntializationException
   *     - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
   *   - backoff will kick in after 1 second, 2s, 4s, 8s, 16s, ...
   *
   */
}
