package com.lifo.sync.rpc

import java.math.BigInteger
import java.util.concurrent.{TimeoutException, TimeUnit}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import scala.util.Try
import org.web3j.abi.TypeDecoder
import org.web3j.protocol.core.{DefaultBlockParameter, Request}
import org.web3j.protocol.core.methods.response.{EthCall, TransactionReceipt}
import org.web3j.protocol.Web3j

class Web3Rpc(nodeEndpoint: String) {

  private val web3jConfig = new Web3jConfig
  private val web3j = Web3j.build(web3jConfig.buildService(nodeEndpoint))
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  def decodeHexData(hexData: String): String = {
    Try(
      {
        val dataWithoutPrefix = hexData.substring(2)
        val offsetHex = dataWithoutPrefix.substring(0, 64)
        val offset = org.web3j.utils.Numeric.toBigInt(offsetHex)
        val dataStartIndex = offset.intValueExact() * 2

        TypeDecoder.decodeUtf8String(dataWithoutPrefix, dataStartIndex).getValue
      }
    ).getOrElse(null)
  }

  def ethBlockNumber(): Long = {
    val futureBlockNumber = web3j.ethBlockNumber().sendAsync()

    try {
      val blockNumberResult = futureBlockNumber.get(2, TimeUnit.SECONDS)
      blockNumberResult.getBlockNumber.longValue()
    } catch {
      case ex: TimeoutException =>
        logger.error("Fetching block number timed out!", ex)
        throw ex
    }
  }

  def getBlocks(blockNumbers: Array[Long], returnFullTransaction: Boolean = true): mutable.Seq[org.web3j.protocol.core.methods.response.EthBlock.Block] = {
    val batch = web3j.newBatch()
    blockNumbers.foreach(blockNumber => {
      batch.add(web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), returnFullTransaction))
    })
    val futureBlocks = batch.sendAsync()
    try {
      val blockResult = futureBlocks.get(20, TimeUnit.SECONDS)
      val ethBlocks = blockResult.getResponses.asScala.map(_.asInstanceOf[org.web3j.protocol.core.methods.response.EthBlock])
      if (ethBlocks.count(_.getError != null) > 0) {
        logger.error("Error fetching blocks")
        ethBlocks.filter(_.getError != null).foreach(ethBlock => {
          logger.error("Error fetching block " + ethBlock.getError.getMessage)
        })
        throw new Exception("Error fetching blocks")
      }
      ethBlocks.map(_.getBlock)
    } catch {
      case ex: TimeoutException =>
        logger.error("Fetching blocks timed out!", ex)
        throw ex
    }
  }


  def getTransactionReceipts(txns: Array[String]): mutable.Seq[TransactionReceipt] = {
    val batch = web3j.newBatch()
    txns.foreach(txn => {
      batch.add(web3j.ethGetTransactionReceipt(txn))
    })
    val futureBlocks = batch.sendAsync()
    try {
      val blockResult = futureBlocks.get(20, TimeUnit.SECONDS)
      val ethReceipts = blockResult.getResponses.asScala.map(_.asInstanceOf[org.web3j.protocol.core.methods.response.EthGetTransactionReceipt])
      if (ethReceipts.count(_.getError != null) > 0) {
        logger.error("Error fetching blocks")
        ethReceipts.filter(_.getError != null).foreach(ethReceipt => {
          logger.error("Error fetching block " + ethReceipt.getError.getMessage)
        })
        throw new Exception("Error fetching blocks")
      }
      ethReceipts.map(_.getResult)
    } catch {
      case ex: TimeoutException =>
        logger.error("Fetching blocks timed out!", ex)
        throw ex
    }
  }

  private def batch[T](call: T => Request[_, EthCall], parms: Array[T], batch: Int = 100): Array[String] = {
    val res = ListBuffer[String]()
    parms.grouped(batch).foreach(b => {
      val batch = web3j.newBatch()
      b.foreach(parm => {
        batch.add(call(parm))
      })
      res ++= batch.send().getResponses.asScala.map(_.asInstanceOf[EthCall].getValue)
    })
    res.toArray
  }
}
