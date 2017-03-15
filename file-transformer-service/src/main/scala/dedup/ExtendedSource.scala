package dedup


import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import crdt.DataBot

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * ExtendedSource is used as Implicit Conversion to add a new method called 'withUniqueId' to {@akka.stream.scaladsl.Source}
  */
case class ExtendedSource[+Out, +Mat](source: Source[Out, Mat]) {

  implicit val timeout = Timeout(5 seconds)

  def withUniqueId(dataBot: ActorRef)(f: Out => Int): Source[Out, Mat] = {

    source
      .filter((elem) => {
        val value = f(elem)
        val future: Future[Boolean] = ask(dataBot, DataBot.MightContain(value)).mapTo[Boolean]
        val result = Await.result(future, timeout.duration)

        if (!result)
          dataBot ! DataBot.Put(value)
        !result
      }
      )

  }
}