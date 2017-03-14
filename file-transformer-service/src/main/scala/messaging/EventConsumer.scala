package messaging

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffsetBatch}
import akka.kafka.scaladsl.Consumer.Control
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory
import crdt.DataBot
import messages.LoadFileCommand
import play.api.libs.json.Json
import repository.InMemoryRepository
import transfer.FileTransformer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class EventConsumer(implicit mat: Materializer) extends Actor with ActorLogging {

  import EventConsumer._

  implicit val ActorSystem = context.system

  val distributedDataBot = context.actorOf(Props(new DataBot))

  private val config = ConfigFactory.load()
  private val csvImporter = new FileTransformer(config, new InMemoryRepository, distributedDataBot)

  override def preStart(): Unit = {
    super.preStart()
    self ! Start
  }

  override def receive: Receive = {
    case Start =>
      log.info("Initializing event consumer")
      val (control, future) = EventSource.create("eventConsumer")(context.system)
        .mapAsync(2)(processMessage)
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

  private def processMessage(msg: Message): Future[Message] = {
    val command=Json.parse(msg.record.value()).as[LoadFileCommand]
    log.info(s"Consumed event: ${command.fileName}")
    csvImporter.transformFile(command.fileName)
    Future.successful(msg)
  }
}

object EventConsumer {
  type Message = CommittableMessage[Array[Byte], String]

  case object Start

  case object Stop

}