import java.nio.file.Paths

import akka.actor.{ActorSystem, Props}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import messaging.EventConsumer
import repository.InMemoryRepository
import transfer.FileTransformer

object Service extends App {

  /*val port = if (args.isEmpty) "0" else args(0)
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.load("application"))*/

  val system = ActorSystem("ClusterSystem")

  implicit val materializer = ActorMaterializer.create(system)

  val eventConsumer = system.actorOf(Props(new EventConsumer))

}
