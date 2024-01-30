package com.lifo.server

import cats.effect._
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.Serialization
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.matching.Regex

object QueryParamMatchers {
  object PageQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")

  object PageSizeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("size")

  object ToAddressQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("address")
}

object Routes {
  val HexStringPattern: Regex = "^[0-9a-fA-F]{40}$".r
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  implicit val anyEncoder: Encoder[Any] = Encoder.instance {
    case value: String => Json.fromString(value)
    case value: Option[Any] => value match {
      case Some(v) => anyEncoder(v)
      case None => Json.Null
    }
    case value: BigDecimal => Json.fromBigDecimal(value)
    case value: java.sql.Timestamp =>

      val timestamp = new Timestamp(System.currentTimeMillis())
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val formattedDate = timestamp.toLocalDateTime.format(formatter)
      Json.fromString(formattedDate)
    case value: Int    => Json.fromInt(value)
    case value: Long   => Json.fromLong(value)
    case value: Double => Json.fromDouble(value).getOrElse(Json.Null)
    case value: Boolean => Json.fromBoolean(value)
    case value: Map[String, Any] => Json.fromFields(value.mapValues(_.asJson).toSeq)
    case value: Array[Map[String, Any]] => Json.fromValues(value.map(_.asJson))
    // Handle other types as needed
    case _             => Json.Null // or some other default
  }
  import QueryParamMatchers._

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@GET -> Root / "v1" / "explorer" / "l1_to_l2_transactions" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize) +& ToAddressQueryParamMatcher(address) =>
      (page, pageSize, address) match {
        case (Some(p), Some(s), ta) if p > 0 && s > 0 && (ta.isEmpty || ta.get.length == 42 && ta.exists(_.substring(2).matches(HexStringPattern.regex))) =>
          val seq = Await.result(DatabaseOperations.getL1ToL2Transactions(p, s, ta), 2 seconds).toArray
          val count = Await.result(DatabaseOperations.getL1ToL2TransactionsCount(ta), 1 seconds)
          Ok(
            Map(
              "data" -> seq.map(_.toMap),
              "total" -> count,
              "page" -> p,
              "size" -> s
            )
          )
        case _ =>
          BadRequest("Invalid request parameters")
      }
    case req@GET -> Root / "v1" / "explorer" / "l2_to_l1_transactions" :? PageQueryParamMatcher(page) +& PageSizeQueryParamMatcher(pageSize) +& ToAddressQueryParamMatcher(address) =>
      (page, pageSize, address) match {
        case (Some(p), Some(s), ta) if p > 0 && s > 0 && (ta.isEmpty || ta.get.length == 42 && ta.exists(_.substring(2).matches(HexStringPattern.regex))) =>
          val seq = Await.result(DatabaseOperations.getL2ToL1Transactions(p, s, ta), 2 seconds).toArray
          val count = Await.result(DatabaseOperations.getL2ToL1TransactionsCount(ta), 1 seconds)
          val finalizedBlockNumber = Await.result(DatabaseOperations.getFinalizedBlockNumber(), 1 seconds).headOption.getOrElse(0L)
          Ok(
            Map(
              "data" -> seq.map(_.toMap(finalizedBlockNumber)),
              "total" -> count,
              "page" -> p,
              "size" -> s
            )
          )
        case _ =>
          BadRequest("Invalid request parameters")
      }
  }
}


