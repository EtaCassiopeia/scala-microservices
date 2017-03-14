import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import messaging.EventConsumer

object Service extends App {

  val system = ActorSystem("ClusterSystem")

  implicit val materializer = ActorMaterializer.create(system)

  val eventConsumer = system.actorOf(Props(new EventConsumer))

}
