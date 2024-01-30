package com.lifo.server

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._

object DatabaseOperations {
  import DatabaseConfig.db

  def getL1ToL2Transactions(page: Int, pageSize: Int, addressFilter: Option[String]): Future[Seq[L1ToL2Transaction]] = {
    val query = TableQuery[L1ToL2Transactions]
      .filter(_.l1BlockNumber.isDefined)
      .filterOpt(addressFilter) { case (table, address) => table.toAddress === address.toLowerCase}
      .sortBy(_.index)
      .drop((page - 1) * pageSize)
      .take(pageSize)

    db.run(query.result)
  }

  def getFinalizedBlockNumber(): Future[Seq[Long]] = {
    val query = TableQuery[OpBedrockStateBatches]
      .sortBy(_.batch_index.desc)
      .map(_.end_block_number)
      .take(1)
    db.run(query.result)
  }
  def getL1ToL2TransactionsCount(addressFilter: Option[String]): Future[Int] = {
    val query = TableQuery[L1ToL2Transactions]
      .filter(_.l1BlockNumber.isDefined)
      .filterOpt(addressFilter) { case (table, address) => table.toAddress === address.toLowerCase}
      .size
    db.run(query.result)
  }

  def getL2ToL1Transactions(page: Int, pageSize: Int, addressFilter: Option[String]): Future[Seq[L2ToL1Transaction]] = {
    val query = TableQuery[L2ToL1Transactions]
      .filter(_.l2BlockNumber.isDefined)
      .filterOpt(addressFilter) { case (table, address) => table.toAddress === address.toLowerCase}
      .sortBy(_.index)
      .drop((page - 1) * pageSize)
      .take(pageSize)

    db.run(query.result)
  }

  def getL2ToL1TransactionsCount(addressFilter: Option[String]): Future[Int] = {
    val query = TableQuery[L2ToL1Transactions]
      .filter(_.l2BlockNumber.isDefined)
      .filterOpt(addressFilter) { case (table, address) => table.toAddress === address.toLowerCase}
      .countDistinct
    db.run(query.result)
  }

}
