package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

/**
 * A simplified voting system
 */
object ChangingActorBehaviourExercises2Improved extends App {
  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    var candidate: Option[String] = None

    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregateVotes(citizen: Set[ActorRef])

  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingStatus(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {
      case VoteStatusReply(None) =>
        // a citizen hasn't voted yet
        sender() ! VoteStatusRequest // this might end up in an infinite loop
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $newStats")
        } else {
          // still need to process some statuses
          context.become(awaitingStatus(newStillWaiting, newStats))
        }
    }

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awaitingStatus(citizens, Map()))
    }
    
    
  }

  val system = ActorSystem("SimplifiedVotingSystem")
  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /**
   * 1. Print the status of the votes
   *    Martin -> 1
   *    Jonas  -> 1
   *    Roland -> 2
   */
}
