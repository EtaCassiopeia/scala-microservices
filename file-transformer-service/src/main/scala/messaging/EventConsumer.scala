package messaging

import akka.actor.{Actor, ActorLogging}
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer.Control
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * EventConsumer is responsible for collection and processing events from Kafka topic.
  * It uses Akka Stream to do this. A plug-able EventHandler can handle received events.
  *
  */
class EventConsumer(eventHandler:EventHandler)(implicit mat: Materializer) extends Actor with ActorLogging {

  import EventConsumer._

  override def preStart(): Unit = {
    super.preStart()
    self ! Start
  }

  override def receive: Receive = {
    case Start =>
      log.info("Initializing event consumer")

      val (control, future) = EventSource.create(ConfigFactory.load.getString("bootstrap.servers"),"eventConsumer")(context.system)
        .mapAsync(2)(eventHandler.processMessage)
        .map(_.committableOffset)
        .groupedWithin(10, 15 seconds)
        .map(group => group.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem) })
        .mapAsync(1)(_.commitScaladsl())
        .toMat(Sink.ignore)(Keep.both)
        .run()

      context.become(running(control))

      future.onFailure {
        case ex =>
          log.error("Stream failed due to error, restarting", ex)
          throw ex
      }

      log.info("Event consumer started")
  }

  def running(control: Control): Receive = {
    case Stop =>
      log.info("Shutting down event consumer stream and actor")
      control.shutdown().andThen {
        case _ =>
          context.stop(self)
      }
  }
}

object EventConsumer {

  case object Start

  case object Stop

}