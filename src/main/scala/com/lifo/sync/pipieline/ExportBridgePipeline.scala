package com.lifo.sync.pipieline

import com.lifo.sync.`type`.{Block, Receipt, Transaction}
import com.lifo.sync.exporters.{BlockExporter, ReceiptExporter}
import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.io.jdbc.DatabaseUtils
import com.lifo.sync.pipieline.filter.TransactionFilter
import com.lifo.sync.rpc.Web3Rpc

class ExportBridgePipeline(
                            web3Provider: Seq[String],
                            val start: Option[Long],
                            val syncFile: String,
                            batchCount: Int,
                            maxBatchRequest: Int,
                            parallelism: Int,
                            filter: TransactionFilter,
                            extractor: BridgeExtractor
                          ) extends Pipeline {
  override val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  override def getCurrentBlockNumber: Long = currentBlockNumber

  lazy private val postgres = DatabaseUtils
  private val web3 = new Web3Rpc(web3Provider.head)
  var currentBlockNumber: Long = readSyncFile(start.getOrElse(0L))

  lazy val blockExporter = new BlockExporter(parallelism, web3Provider, maxBatchRequest)
  lazy val receiptExporter = new ReceiptExporter(parallelism, web3Provider, maxBatchRequest)

  def processBlocks(latestBlockNumber: Long, end: Option[Long]): Boolean = {
    if (currentBlockNumber >= latestBlockNumber) {
      logger.info(s"Latest block: $latestBlockNumber; Current block: $currentBlockNumber. Waiting for new blocks.")
      Thread.sleep(3000)
      true
    } else {
      val nextBlockNumber = Math.min(currentBlockNumber + batchCount, latestBlockNumber)
      logger.info(s"Latest block: $latestBlockNumber; Exporting blocks: $currentBlockNumber to $nextBlockNumber.")

      val transactionInfos = filter.rangeFilterByEvents(currentBlockNumber, nextBlockNumber)

      logger.info(s"contract: ${filter.contractAddress}, transactions.length: ${transactionInfos.length}")

      val blocks = blockExporter.exportAll(transactionInfos.map(_.blockNumber).distinct)
      val receipts = receiptExporter.exportAll(transactionInfos.map(_.hash))

      val transactions = enrich(blocks, receipts)
      val data = extractor.extract(transactions)
      if (data.flatten.isEmpty) {
        logger.info(s"Exporting blocks: $currentBlockNumber to $nextBlockNumber. No data to export.")
      } else {
        val t0 = System.currentTimeMillis()
        logger.info(s"Saving to postgresql Begin")
        data.filter(_.nonEmpty).foreach(x => postgres.saveToPostgres(x))
        val t1 = System.currentTimeMillis()
        logger.info(s"Saving to postgresql End, takes ${(t1 - t0) / 1e3} s")
      }
      currentBlockNumber = nextBlockNumber
      writeSyncFile(currentBlockNumber)
      end.forall(_ > currentBlockNumber)
    }
  }

  override def execute(endCondition: () => Boolean): Unit = {
    while (endCondition()) {
      if (!processBlocks(web3.ethBlockNumber() - 1, None)) return
    }
  }

  private def enrich(
                      blocks: Array[Block],
                      receipts: Array[Receipt],
                    ): (
    Array[Transaction]
    ) = {
    val blockTimestampMap = blocks.map(x => (x.number, x.timestamp)).toMap
    val receiptsMap = receipts.map(x => (x.transactionHash, x)).toMap
    blocks.flatMap(_.transactions).filter(receipts.map(_.transactionHash) contains _.hash).map(
      x => {
        x.blockTimestamp = blockTimestampMap(x.blockNumber)
        x.receipt = receiptsMap(x.hash)
        x
      }
    )
  }
}
