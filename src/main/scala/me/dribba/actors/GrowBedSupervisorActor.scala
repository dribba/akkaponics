package me.dribba.actors

import akka.actor.{Props, ActorLogging, ActorRef, Actor}
import me.dribba.models.aquaponics.GrowBed
import me.dribba.providers.GrowBedActorFactory

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration


class GrowBedSupervisorActor(
                         beds: List[GrowBed],
                         cycleEvery: FiniteDuration,
                         growBedFactory: GrowBedActorFactory
                         ) extends Actor with ActorLogging {

  import context._

  private var activeBeds = Set.empty[ActorRef]

  private var knownBeds = Map.empty[ActorRef, GrowBed]

  private var queue = Queue.empty[ActorRef]

  private def nextTick = cycleEvery / activeBeds.size

  protected[actors] def scheduleNext(): Unit = context.system.scheduler.scheduleOnce(nextTick, self, FlushNext)

  protected[actors] def start(): Unit = self ! FlushNext

  protected[actors] def createBedActor(growBed: GrowBed): ActorRef =
    growBedFactory.create(context, self, growBed)


  protected[actors] def removeActiveBed(bedActor: ActorRef) = {
    queue = queue.filter(_ != bedActor)
    activeBeds -= bedActor
  }

  private def getActiveBeds: List[GrowBed] =
    knownBeds.filterKeys(activeBeds(_)).values.toList


  override def preStart() = {
    // create beds
    beds.foreach(bed => {
      val bedActor = createBedActor(bed)
      knownBeds += bedActor -> bed
      activeBeds += bedActor
      queue :+= bedActor
    })

    start()
  }

  override def receive: Receive = {
    case m: GrowBedSupervisorMessage => m match {
      case FlushNext =>
        scheduleNext()

        queue.dequeueOption match {
          case None =>
            log.info("Nothing to flush")
          case Some((bed, newQueue)) =>
            log.info("Telling {} to flush", knownBeds(bed))
            bed ! Flush
            queue = newQueue.enqueue(bed)
        }

      case GetActiveBeds =>
        sender() ! ActiveBeds(getActiveBeds)
    }

    case m: GrowBedMessage => m match {
      case OutOfService =>
        removeActiveBed(sender())
    }
  }

}

object GrowBedSupervisorActor {

  def props(beds: List[GrowBed], cycleEvery: FiniteDuration, growBedFactory: GrowBedActorFactory): Props =
    Props(classOf[GrowBedSupervisorActor], beds, cycleEvery, growBedFactory)

}


sealed trait GrowBedSupervisorMessage

private object FlushNext extends GrowBedSupervisorMessage
object GetActiveBeds extends GrowBedSupervisorMessage
case class ActiveBeds(beds: List[GrowBed]) extends GrowBedSupervisorMessage