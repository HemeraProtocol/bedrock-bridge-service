package com.lifo.sync.io.jdbc.`type`

import java.sql.Timestamp
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write


abstract class PostgresqlRow {
  import java.util.{Calendar, TimeZone}
  val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

  def tableName: String = this.getClass.getSimpleName.toLowerCase + "s"

  def onConflictSQL(tableName: String): String = " ON CONFLICT DO NOTHING"


  def setPreparedStatement(ps: java.sql.PreparedStatement): Unit = {
    val clazz = this.getClass
    val fields = clazz.getDeclaredFields
    fields.zipWithIndex.foreach { case (field, index) =>
      field.setAccessible(true)
      field.get(this) match {
        case b: Array[Byte] =>
          ps.setBytes(index + 1, b)
        case json: Map[String, _] =>
          import org.postgresql.util.PGobject
          val jsonObject = new PGobject
          jsonObject.setType("json")
          implicit val formats: Formats = Serialization.formats(NoTypeHints)
          jsonObject.setValue(write(json))
          ps.setObject(index + 1, jsonObject)
        case l if field.getName.contains("timestamp") =>
          ps.setTimestamp(index + 1, new Timestamp(l.asInstanceOf[Long] * 1000))
        case Some(v) =>
          ps.setObject(index + 1, v)
        case None =>
          ps.setObject(index + 1, null)
        case other =>
          ps.setObject(index + 1, other)
      }

    }
  }

  def columns(): Seq[String] = this.getClass.getDeclaredFields.map(_.getName)

  def insertSQLBase(tableName: String, schemaName: String = "public"): String = {
    s"INSERT INTO ${schemaName}.${tableName} (${columns().mkString(", ")}) VALUES (${columns().map(_ => "?").mkString(", ")})"
  }
}

