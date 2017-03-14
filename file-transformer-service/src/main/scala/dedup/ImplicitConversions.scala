package dedup

import akka.stream.scaladsl.Source

object ImplicitConversions {

  implicit def toUniqueSource[Out, Mat](source: Source[Out, Mat]): ExtendedSource[Out, Mat] = ExtendedSource(source)

}
