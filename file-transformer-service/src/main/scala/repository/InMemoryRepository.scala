package repository

import slick.driver.H2Driver.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}
import transfer.Record

class InMemoryRepository {
  val db = Database.forConfig("h2mem1")

  val activities: TableQuery[Activities] = TableQuery[Activities]

  val setup = DBIO.seq(
    activities.schema.create
  )

  val setupFuture = db.run(setup)

  def batchInsertIntoDb(records: Seq[Record]) =
    db run (activities ++= records.map(r => (r.id, r.name, r.time_of_start)))

  def insertIntoDb(record: Record) =
    db run (activities += (record.id, record.name, record.time_of_start))
}


class Activities(tag: Tag) extends Table[(Int, String, String)](tag, "ACTIVITIES") {
  def id = column[Int]("ACT_ID", O.PrimaryKey)

  // This is the primary key column
  def name = column[String]("USER_NAME")

  def startTime = column[String]("START_TIME")

  def * = (id, name, startTime)
}