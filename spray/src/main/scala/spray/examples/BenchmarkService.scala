package spray.examples

import akka.actor._
import scala.concurrent.duration._
import spray.can.Http
import spray.json._
import spray.http._
import MediaTypes._
import HttpMethods._
import StatusCodes._
import util.{Try, Random}
import DefaultJsonProtocol._
import concurrent.Future

class BenchmarkService(dbPool: DBSessionPool) extends Actor {
  import context.dispatcher
  import Uri._
  import Uri.Path._

  def fastPath: Http.FastPath = {
    case HttpRequest(GET, Uri(_, _, Slash(Segment("json", Path.Empty)), _, _), _, _, _) =>
      val json = JsObject("message" -> JsString("Hello, World!"))
      jsonResponse(json)
  }

  val random = new Random()

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self, fastPath = fastPath)

    case HttpRequest(GET, Path("/"), _, _, _) => sender ! HttpResponse(
      entity = HttpEntity(MediaTypes.`text/html`,
        <html>
          <body>
            <h1>Tiny <i>spray-can</i> benchmark server</h1>
            <p>Defined resources:</p>
            <ul>
              <li><a href="/json">/json</a></li>
              <li><a href="/stop">/stop</a></li>
            </ul>
          </body>
        </html>.toString()
      )
    )

    case HttpRequest(GET, Uri(_, _, Slash(Segment("db", Path.Empty)), Query.Empty, _), _, _, _) =>
      val commander = sender
      oneDbRequest.foreach { result =>
        commander ! jsonResponse(result.toJson)
      }

    case HttpRequest(GET, Uri(_, _, Slash(Segment("db", Path.Empty)), Query.Cons("queries", num, Query.Empty), _), _, _, _) =>
      val numQueries = Try(num.toInt).getOrElse(1) match {
        case x if x < 1 => 1
        case x if x > 500 => 500
        case x => x
      }

      val commander = sender
      Future.sequence(Seq.fill(numQueries)(oneDbRequest)).foreach { results =>
        commander ! jsonResponse(results.toJson)
      }

    case HttpRequest(GET, Path("/stop"), _, _, _) =>
      sender ! HttpResponse(entity = "Shutting down in 1 second ...")
      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }

    case _: HttpRequest => sender ! HttpResponse(NotFound, entity = "Unknown resource!")
  }

  def oneDbRequest: Future[World] = {
    import slick.driver.MySQLDriver.simple._
    val id = random.nextInt(10000)
    dbPool.runQuery(World.byId(id)).map(_.head)
  }

  def jsonResponse(json: JsValue): HttpResponse =
    HttpResponse(entity = HttpEntity(ContentType.`application/json`, json.compactPrint))
}
