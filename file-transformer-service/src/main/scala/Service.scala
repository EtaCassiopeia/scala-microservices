import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import messaging.{CsvImporterEventHandler, EventConsumer}

import scala.concurrent.Await
import scala.concurrent.duration._

object Service extends App {

  implicit val system = ActorSystem("ClusterSystem")

  scala.sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, 30 seconds)
  }

  implicit val materializer = ActorMaterializer.create(system)

  system.actorOf(Props(new EventConsumer(new CsvImporterEventHandler)))

}
