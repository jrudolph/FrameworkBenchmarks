package spray.examples

import slick.session.Database
import slick.driver.MySQLDriver.simple._

case class World(id: Int, randomNumber: Int)

object World extends Table[World]("World") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def randomNumber = column[Int]("randomNumber")
  def * = id ~ randomNumber <> (World.apply(_, _), World.unapply _)

  val byId =
    for {
      id <- Parameters[Int]
      w <- World if w.id is id
    } yield w

  import spray.json.DefaultJsonProtocol._
  implicit val worldFormat = jsonFormat(World.apply, "id", "randomNumber")
}
