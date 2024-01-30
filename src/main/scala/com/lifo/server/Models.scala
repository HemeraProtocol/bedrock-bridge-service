package com.lifo.server


import java.sql.Timestamp
import java.time.{ZonedDateTime, ZoneId}
import slick.jdbc.PostgresProfile.api._

case class L1ToL2Transaction(
                              version: Int,
                              index: Long,
                              l1FromAddress: Option[String],
                              l1BlockNumber: Option[Long],
                              l1BlockTimestamp: Option[Timestamp],
                              l1TransactionHash: Option[String],
                              l1TokenAddress: Option[String],
                              l2BlockNumber: Option[Long],
                              l2BlockTimestamp: Option[Timestamp],
                              l2TransactionHash: Option[String],
                              l2TokenAddress: Option[String],
                              fromAddress: Option[String],
                              toAddress: Option[String],
                              amount: Option[BigDecimal]
                            ) {
  def toMap: Map[String, Any] = Map(
    "version" -> version,
    "index" -> index,
    "l1_from_address" -> l1FromAddress,
    "l1_block_number" -> l1BlockNumber,
    "l1_block_timestamp" -> l1BlockTimestamp,
    "l1_transaction_hash" -> l1TransactionHash,
    "l1_token_address" -> l1TokenAddress,
    "l2_block_number" -> l2BlockNumber,
    "l2_block_timestamp" -> l2BlockTimestamp,
    "l2_transaction_hash" -> l2TransactionHash,
    "l2_token_address" -> l2TokenAddress,
    "from_address" -> fromAddress,
    "to_address" -> toAddress,
    "amount" -> amount
  )
}

class L1ToL2Transactions(tag: Tag) extends Table[L1ToL2Transaction](tag, "l1_to_l2_txns") {
  def version = column[Int]("version", O.PrimaryKey)

  def index = column[Long]("index", O.PrimaryKey)

  def l1FromAddress = column[Option[String]]("l1_from_address")

  def l1BlockNumber = column[Option[Long]]("l1_block_number")

  def l1BlockTimestamp = column[Option[Timestamp]]("l1_block_timestamp")


  def l1TransactionHash = column[Option[String]]("l1_transaction_hash")

  def l1TokenAddress = column[Option[String]]("l1_token_address")

  def l2BlockNumber = column[Option[Long]]("l2_block_number")

  def l2BlockTimestamp = column[Option[Timestamp]]("l2_block_timestamp")


  def l2TransactionHash = column[Option[String]]("l2_transaction_hash")

  def l2TokenAddress = column[Option[String]]("l2_token_address")

  def fromAddress = column[Option[String]]("from_address")

  def toAddress = column[Option[String]]("to_address")

  def amount = column[Option[BigDecimal]]("amount")

  def * = (version, index, l1FromAddress, l1BlockNumber, l1BlockTimestamp, l1TransactionHash, l1TokenAddress, l2BlockNumber, l2BlockTimestamp, l2TransactionHash, l2TokenAddress, fromAddress, toAddress, amount) <> (L1ToL2Transaction.tupled, L1ToL2Transaction.unapply)
}

case class L2ToL1Transaction(
                              version: Int,
                              index: Long,
                              l2FromAddress: Option[String],
                              l2BlockNumber: Option[Long],
                              l2BlockTimestamp: Option[Timestamp],
                              l2TransactionHash: Option[String],
                              l2TokenAddress: Option[String],
                              l1TokenAddress: Option[String],
                              fromAddress: Option[String],
                              toAddress: Option[String],
                              amount: Option[BigDecimal],
                              l1ProvenBlockNumber: Option[Long],
                              l1ProvenBlockTimestamp: Option[Timestamp],
                              l1ProvenTransactionHash: Option[String],
                              l1FinalizedBlockNumber: Option[Long],
                              l1FinalizedBlockTimestamp: Option[Timestamp],
                              l1FinalizedTransactionHash: Option[String],
                            ) {

  def plusDays(ts: Timestamp, days: Int): ZonedDateTime = {
    ts.toInstant.atZone(ZoneId.of("UTC")).plusDays(days)
  }

  def getType(fn: Long, expiredDay: Int = 3): Int = {
    if (fn < l2BlockNumber.get){
      1 // Transaction not finalized
    } else {
      l1ProvenTransactionHash.isEmpty match {
        case true => 2 // Ready to Prove
        case false if l1ProvenBlockTimestamp.exists(timestamp => ZonedDateTime.now().isBefore(plusDays(timestamp, days = expiredDay))) => 3 // In Challenge Period
        case false if l1ProvenBlockTimestamp.exists(timestamp => ZonedDateTime.now().isAfter(plusDays(timestamp, days = expiredDay))) && l1FinalizedTransactionHash.isEmpty => 4 // Ready to Finalize
        case false if l1FinalizedTransactionHash.isDefined => 5 // Relayed
        case _ => throw new RuntimeException("Unknown state")
      }
    }
  }

  def toMap(finalizedBlockNumber: Long): Map[String, Any] = Map(
    "type" -> getType(finalizedBlockNumber),
    "version" -> version,
    "index" -> index,
    "l2_from_address" -> l2FromAddress,
    "l2_block_number" -> l2BlockNumber,
    "l2_block_timestamp" -> l2BlockTimestamp,
    "l2_transaction_hash" -> l2TransactionHash,
    "l2_token_address" -> l2TokenAddress,
    "l1_token_address" -> l1TokenAddress,
    "from_address" -> fromAddress,
    "to_address" -> toAddress,
    "amount" -> amount,
    "l1_proven_block_number" -> l1ProvenBlockNumber,
    "l1_proven_block_timestamp" -> l1ProvenBlockTimestamp,
    "l1_proven_transaction_hash" -> l1ProvenTransactionHash,
    "l1_finalized_block_number" -> l1FinalizedBlockNumber,
    "l1_finalized_block_timestamp" -> l1FinalizedBlockTimestamp,
    "l1_finalized_transaction_hash" -> l1FinalizedTransactionHash
  )
}

class L2ToL1Transactions(tag: Tag) extends Table[L2ToL1Transaction](tag, "l2_to_l1_txns") {
  def version = column[Int]("version", O.PrimaryKey)

  def index = column[Long]("index", O.PrimaryKey)

  def l2FromAddress = column[Option[String]]("l2_from_address")

  def l2BlockNumber = column[Option[Long]]("l2_block_number")

  def l2BlockTimestamp = column[Option[Timestamp]]("l2_block_timestamp")

  def l2TransactionHash = column[Option[String]]("l2_transaction_hash")

  def l2TokenAddress = column[Option[String]]("l2_token_address")

  def l1TokenAddress = column[Option[String]]("l1_token_address")

  def fromAddress = column[Option[String]]("from_address")

  def toAddress = column[Option[String]]("to_address")

  def amount = column[Option[BigDecimal]]("amount")

  def l1ProvenBlockNumber = column[Option[Long]]("l1_proven_block_number")

  def l1ProvenBlockTimestamp = column[Option[Timestamp]]("l1_proven_block_timestamp")

  def l1ProvenTransactionHash = column[Option[String]]("l1_proven_transaction_hash")

  def l1FinalizedBlockNumber = column[Option[Long]]("l1_finalized_block_number")

  def l1FinalizedBlockTimestamp = column[Option[Timestamp]]("l1_finalized_block_timestamp")

  def l1FinalizedTransactionHash = column[Option[String]]("l1_finalized_transaction_hash")

  def * = (
    version, index, l2FromAddress, l2BlockNumber, l2BlockTimestamp, l2TransactionHash, l2TokenAddress, l1TokenAddress, fromAddress, toAddress, amount, l1ProvenBlockNumber, l1ProvenBlockTimestamp, l1ProvenTransactionHash, l1FinalizedBlockNumber, l1FinalizedBlockTimestamp, l1FinalizedTransactionHash
  ) <> (L2ToL1Transaction.tupled, L2ToL1Transaction.unapply)
}

case class OpBedrockStateBatch(
                                batch_index: Long,
                                l1_block_number: Long,
                                l1_block_timestamp: Timestamp,
                                l1_block_hash: String,
                                l1_transaction_hash: String,
                                start_block_number: Long,
                                end_block_number: Long,
                                batch_root: String,
                                block_count: Int
                              )

class OpBedrockStateBatches(tag: Tag) extends Table[OpBedrockStateBatch](tag, "op_bedrock_state_batches") {
  def batch_index = column[Long]("batch_index", O.PrimaryKey)

  def l1_block_number = column[Long]("l1_block_number")

  def l1_block_timestamp = column[Timestamp]("l1_block_timestamp")

  def l1_block_hash = column[String]("l1_block_hash")

  def l1_transaction_hash = column[String]("l1_transaction_hash")

  def start_block_number = column[Long]("start_block_number")

  def end_block_number = column[Long]("end_block_number")

  def batch_root = column[String]("batch_root")

  def block_count = column[Int]("block_count")

  def * = (batch_index, l1_block_number, l1_block_timestamp, l1_block_hash, l1_transaction_hash, start_block_number, end_block_number, batch_root, block_count) <> (OpBedrockStateBatch.tupled, OpBedrockStateBatch.unapply)
}
