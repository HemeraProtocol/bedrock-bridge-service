package com.lifo.sync.pipieline

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source

trait Pipeline {

  val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  val syncFile: String
  val start: Option[Long]
  def getCurrentBlockNumber: Long = ???

  def execute(endCondition: () => Boolean): Unit = ???

  def start(end: Long): Unit = {
    execute(() => getCurrentBlockNumber < end)
  }

  def startStreaming(): Unit = {
    execute(() => true)
  }

  def readSyncFile(start: Long = 0L): Long = {
    try {
      val blockNumber = Source.fromFile(syncFile).getLines().next().trim().toLong
      blockNumber
    } catch {
      case e: FileNotFoundException =>
        logger.warn(s"Sync file not found, creating a new one with start block as 0: ${e.getMessage}")
        writeSyncFile(start)
        start
      case e: Exception =>
        logger.error(s"Error reading sync file: ${e.getMessage}")
        throw e
    }
  }

  def writeSyncFile(blockNumber: Long): Unit = {
    try {
      val blockNumberStr = blockNumber.toString
      Files.write(Paths.get(syncFile), blockNumberStr.getBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    } catch {
      case e: Exception =>
        println(s"Error writing to sync file: ${e.getMessage}")
    }
  }
}
