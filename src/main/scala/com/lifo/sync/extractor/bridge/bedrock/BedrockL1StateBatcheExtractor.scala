package com.lifo.sync.extractor.bridge.bedrock

import com.lifo.sync.`type`.Transaction
import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.io.jdbc.`type`.bridge.bedrock.StateBatchesOnL1
import com.lifo.sync.io.jdbc.`type`.PostgresqlRow
import com.lifo.sync.utils.Utils.bytesToHex
import scala.collection.mutable.ListBuffer

object BedrockBatchesExtractor extends BridgeExtractor {

  override def extract(transactions: Array[Transaction]): ListBuffer[Array[PostgresqlRow]] = {
    val stateBatches = transactions.filter(_.receipt.logs.exists(_.topic0 == outputProposedEvent.signature))
      .flatMap(parseProposeL2Output)
    ListBuffer(stateBatches.asInstanceOf[Array[PostgresqlRow]])
  }

  def parseProposeL2Output(transaction: Transaction): Array[StateBatchesOnL1] = {
    transaction.receipt.logs.filter(_.topic0 == outputProposedEvent.signature).map(
      x=> {
        val outputProposed = outputProposedEvent.decodeLogEvent(x).get
        StateBatchesOnL1(
          batch_index = outputProposed("l2OutputIndex").toString.toLong,
          l1_block_number = transaction.blockNumber,
          l1_block_timestamp = transaction.blockTimestamp,
          l1_block_hash = transaction.blockHash,
          l1_transaction_hash =transaction.hash,
          end_block_number = outputProposed("l2BlockNumber").toString.toLong,
          batch_root ="0x" + bytesToHex(outputProposed("outputRoot").asInstanceOf[Array[Byte]]),
        )
      }
    )
  }
}
