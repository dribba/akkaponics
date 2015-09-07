package me.dribba.actors

import akka.actor.{Props, ActorLogging, ActorRef, Actor}
import com.typesafe.config.Config
import me.dribba.models.aquaponics.GrowBed
import me.dribba.providers.GrowBedActorFactory

import scala.collection.immutable.Queue
import scala.concurrent.duration._

import me.dribba.utils.DurationUtils._

import scala.util.Try

class AllAtOnceSupervisorActor(
                                beds: List[GrowBed],
                                cycleEvery: FiniteDuration,
                                growBedFactory: GrowBedActorFactory,
                                config: Config
                                ) extends Actor with ActorLogging {

  import context._
  import AllAtOnceSupervisorActor._

  implicit val _ = config

  private var activeBeds = Set.empty[ActorRef]

  private var knownBeds = Map.empty[ActorRef, GrowBed]

  private var queue = Queue.empty[ActorRef]

  private val interval = Try(finiteFromConfig("akkaponics.bedInterval")).getOrElse(5 minutes)
  private val flushAllInterval = Try(finiteFromConfig("akkaponics.totalFlushCycle")).getOrElse(1 hour)

  private def nextTick(growBed: GrowBed): FiniteDuration =
    growBed.flushTime + interval

  protected[actors] def scheduleNext(growBed: GrowBed, reminder: Queue[ActorRef]): Unit =
    context.system.scheduler.scheduleOnce(nextTick(growBed), self, AllAtOnceSupervisorActor.FlushNext(reminder))

  protected[actors] def scheduleFlushAll(): Unit =
    context.system.scheduler.scheduleOnce(flushAllInterval, self, AllAtOnceSupervisorActor.FlushAll)

  protected[actors] def start(): Unit = {
    log.info("Starting all at once supervisor")
    self ! AllAtOnceSupervisorActor.FlushAll
  }

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
    case AllAtOnceSupervisorActor.FlushAll =>

      scheduleFlushAll()
      self ! AllAtOnceSupervisorActor.FlushNext(queue)

    case AllAtOnceSupervisorActor.FlushNext(reminder) =>
      reminder.dequeueOption match {
        case None =>
          log.info("Nothing to flush")

        case Some((bed, newQueue)) =>

          val growBed = knownBeds(bed)

          scheduleNext(growBed, newQueue)
          log.info("Telling {} to flush", growBed)
          bed ! Flush
      }


    case GetActiveBeds =>
      sender() ! ActiveBeds(getActiveBeds)

    case OutOfService =>
      removeActiveBed(sender())
  }

}

object AllAtOnceSupervisorActor {

  def props(beds: List[GrowBed], cycleEvery: FiniteDuration, growBedFactory: GrowBedActorFactory, config: Config): Props =
    Props(classOf[AllAtOnceSupervisorActor], beds, cycleEvery, growBedFactory, config)


  sealed trait AllAtOnceSupervisorMessage

  private case class FlushNext(reminder: Queue[ActorRef]) extends AllAtOnceSupervisorMessage

  private object FlushAll extends AllAtOnceSupervisorMessage

  case class ActiveBeds(beds: List[GrowBed]) extends AllAtOnceSupervisorMessage

}


