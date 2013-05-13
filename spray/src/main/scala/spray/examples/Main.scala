package spray.examples

import akka.actor._
import akka.io.IO
import spray.can.Http
import slick.session.Database

object Main extends App {

  implicit val system = ActorSystem()

  val dbHost = system.settings.config.getString("app.db.host")
  val dbName = system.settings.config.getString("app.db.database")
  val numConns = system.settings.config.getInt("app.db.pool-size")

  val db = Database.forURL("jdbc:mysql://192.168.100.21:3306/hello_world",
                    driver = "com.mysql.jdbc.Driver",
                    user="benchmarkdbuser",
                    password="benchmarkdbpass")

  // the handler actor replies to incoming HttpRequests
  val dbPool = new DBSessionPool(db, system, numConns)
  val handler = system.actorOf(Props(new BenchmarkService(dbPool)), name = "handler")

  val interface = system.settings.config.getString("app.interface")
  val port = system.settings.config.getInt("app.port")
  IO(Http) ! Http.Bind(handler, interface, port)
}
