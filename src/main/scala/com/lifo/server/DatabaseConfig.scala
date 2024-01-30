package com.lifo.server

import scala.util.Properties
import slick.jdbc.PostgresProfile.api._

object DatabaseConfig {
  private def getEnv(name: String, default: String): String = Properties.envOrElse(name, default)

  val db = Database.forURL(
    url = getEnv("POSTGRESQL_URL", "jdbc:postgresql://localhost:5432/yourdb"),
    user = getEnv("POSTGRESQL_USERNAME", "username"),
    password = getEnv("POSTGRESQL_PASSWORD", "password"),
    driver = "org.postgresql.Driver"
  )
}