package com.lifo.server

import cats.data.Kleisli
import cats.effect._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.HttpRoutes
import org.http4s.Status.InternalServerError
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.global

object SocialscanServer extends IOApp {

  val logger = LoggerFactory.getLogger(getClass)
  import org.http4s.server.middleware.ErrorAction
  import org.http4s.server.middleware.ErrorHandling
  override def run(args: List[String]): IO[ExitCode] = {
    val httpApp = Router("/" -> Routes.service).orNotFound

    val withErrorLogging = ErrorHandling(
      ErrorAction.log(
        httpApp,
        messageFailureLogAction = (t, msg) =>
          IO.println(msg) >>
            IO.println(t),
        serviceErrorLogAction = (t, msg) =>
          IO.println(msg) >>
            IO.println(t)
      )
    )

    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(withErrorLogging)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
