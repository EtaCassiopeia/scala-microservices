package messaging

import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import com.typesafe.config.ConfigFactory
import crdt.DataBot
import messages.LoadFileCommand
import play.api.libs.json.Json
import repository.InMemoryRepository
import transfer.FileTransformer

import scala.concurrent.Future

trait EventHandler {
  type Message = CommittableMessage[Array[Byte], String]

  val system: ActorSystem

  def processMessage(msg: Message): Future[Message]
}

/**
  * An implementation of EventHandler which is able to process a file's context and store the results on a Database
  */
class CsvImporterEventHandler(implicit val system: ActorSystem) extends EventHandler {

  val distributedDataBot = system.actorOf(Props(new DataBot))

  private val config = ConfigFactory.load()
  private val csvImporter = new FileTransformer(config, new InMemoryRepository, distributedDataBot)

  override def processMessage(msg: Message): Future[Message] = {
    val command = Json.parse(msg.record.value()).as[LoadFileCommand]
    csvImporter.transformFile(command.fileName)
    Future.successful(msg)
  }
}