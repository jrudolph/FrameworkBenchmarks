package spray.examples

import akka.actor.{Props, ActorSystem, Actor}
import akka.pattern.ask
import slick.session.Database
import slick.lifted.Query
import slick.driver.MySQLDriver.simple._
import concurrent.Future
import akka.util.Timeout
import concurrent.duration._
import akka.routing.RoundRobinRouter
import slick.jdbc.{UnitInvoker, Invoker}

class DBSession(db: Database) extends Actor {
  implicit val session = db.createSession()

  def receive = {
    case q: UnitInvoker[_] =>
      val result = q.list()
      sender ! result
  }


  override def preRestart(reason: Throwable, message: Option[Any]) {
    session.close()
  }
  override def postStop(): Unit = {
    session.close()
  }
}

class DBSessionPool(db: Database, system: ActorSystem, numConnections: Int) {
  val session = system.actorOf(
    Props(new DBSession(db))
      .withRouter(RoundRobinRouter(nrOfInstances = numConnections)))

  implicit val timeout = Timeout(10.seconds)

  def runQuery[T](query: UnitInvoker[T]): Future[Seq[T]] =
    (session ? query).mapTo[Seq[T]]
}
