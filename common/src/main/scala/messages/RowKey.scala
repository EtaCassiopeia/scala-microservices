package messages

import java.util.UUID

case class RowKey(id: UUID)

object RowKey {

  def generate = RowKey(UUID.randomUUID)

}
