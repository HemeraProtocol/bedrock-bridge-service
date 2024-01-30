package com.lifo.sync.io.jdbc

import com.lifo.sync.io.jdbc.`type`.{bridge, PostgresqlRow}
import com.lifo.sync.io.jdbc.`type`.bridge.bedrock.StateBatchesOnL1
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.sql.{Connection, PreparedStatement}
import java.util.concurrent.Executors
import org.flywaydb.core.Flyway
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object DatabaseUtils {

  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(sys.env.getOrElse("POSTGRESQL_URL", ""))
  hikariConfig.setUsername(sys.env.getOrElse("POSTGRESQL_USERNAME", ""))
  hikariConfig.setPassword(sys.env.getOrElse("POSTGRESQL_PASSWORD", ""))
  hikariConfig.setMaximumPoolSize(10)
  hikariConfig.setMinimumIdle(2)

  private val dataSource = new HikariDataSource(hikariConfig)
  private val maxBatchSize: Int = 2000
  private val flyway: Flyway = Flyway.configure
    .dataSource(dataSource)
    .locations("classpath:migrations/postgresql")
    .baselineOnMigrate(true)
    .load()

  flyway.migrate()
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def getConnection: Connection = DatabaseUtils.dataSource.getConnection

  val threadPool = Executors.newFixedThreadPool(5)
  implicit val customExecutionContext = ExecutionContext.fromExecutor(threadPool)

  def withRetry[T](times: Int)(f: => Future[T]): Future[T] = {
    f recoverWith {
      case e if times > 1 =>
        logger.warn(s"Encountered an error, retrying: ${e.getMessage}")
        withRetry(times - 1)(f)
    }
  }

  def enrichFromDB(array: Array[StateBatchesOnL1], conn: Connection): Array[PostgresqlRow] = {
    conn.setAutoCommit(false)
    val stmt = conn.createStatement
    if (array.head.batch_index != 0) {
      val getBeginBlockNumberSql = s"SELECT end_block_number FROM op_bedrock_state_batches WHERE batch_index = ${array.head.batch_index - 1}"
      val rs2 = stmt.executeQuery(getBeginBlockNumberSql)
      var beginBlockNumber = 0L
      if (rs2.next) {
        beginBlockNumber = rs2.getLong("end_block_number")
      } else {
        throw new Exception(s"cannot find begin block number for index ${array.head.batch_index - 1}")
      }
      array.head.start_block_number = beginBlockNumber + 1
    }
    for (i <- 1 until array.length) {
      array(i).start_block_number = array(i - 1).end_block_number + 1
    }
    conn.close()
    array.asInstanceOf[Array[PostgresqlRow]]
  }

  def saveToPostgres[T <: PostgresqlRow](rows: Array[T]): Unit = {

    val enrichRows = (rows.headOption match {
      case Some(_: bridge.bedrock.StateBatchesOnL1) => enrichFromDB(rows.asInstanceOf[Array[bridge.bedrock.StateBatchesOnL1]].sortBy(_.batch_index), getConnection)
      case _ => rows
    })

    val tableName = enrichRows.headOption.get.tableName

    val t0 = System.currentTimeMillis()
    logger.info(s"Saving to ${tableName} begin")

    val futures = enrichRows.grouped(maxBatchSize).map { batch =>
      withRetry(2) {
        Future {
          saveBatchToPostgres(batch, tableName)
        }
      }
    }.toList

    val combinedFuture = Future.sequence(futures)

    try {
      val results = Await.result(combinedFuture, Duration.Inf)
      val t1 = System.currentTimeMillis()
      logger.info(s"${enrichRows.length} rows saved successfully to ${tableName}, takes ${(t1 - t0) / 1e3} s")
    } catch {
      case exception: Throwable =>
        logger.error(s"Error saving batches to ${tableName}", exception)
        throw exception
    }

  }

  private def saveBatchToPostgres[T <: PostgresqlRow](batch: Array[T], tableName: String): Unit = {
    val t0 = System.currentTimeMillis()

    val connection: Connection = getConnection
    val sql = if (batch.nonEmpty) batch.head.insertSQLBase(tableName) + batch.head.onConflictSQL(tableName) else return
    val preparedStatement: PreparedStatement = connection.prepareStatement(sql)

    try {
      connection.setAutoCommit(false)

      for (row <- batch) {
        row.setPreparedStatement(preparedStatement)
        preparedStatement.addBatch()
      }

      preparedStatement.executeBatch()
      connection.commit()

      val t1 = System.currentTimeMillis()
      logger.debug(s"Saving batch to ${tableName} completed in ${(t1 - t0) / 1e3} s")
    } catch {
      case ex: Exception =>
        logger.error(s"Error saving batch to ${tableName}", ex)
        connection.rollback()
        connection.close()
        throw ex
    } finally {
      preparedStatement.close()
      connection.close()
    }
  }

}
