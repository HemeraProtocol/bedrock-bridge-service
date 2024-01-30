package com.lifo.sync.transfer

import com.lifo.sync.`type`
import com.lifo.sync.utils.Utils.hexStringToLong
import scala.collection.JavaConverters._
import scala.language.implicitConversions


object Web3Block {

  private def getOrElse[T](arr: Array[T], index: Int, default: T): T = {
    if (index >= 0 && index < arr.length) arr(index) else default
  }

  def web3Receipt2Receipt(receipt: org.web3j.protocol.core.methods.response.TransactionReceipt): `type`.Receipt = {
    `type`.Receipt(
      receipt.getTransactionHash,
      receipt.getTransactionIndex.toString.toInt,
      util.Try(receipt.getCumulativeGasUsed.longValue()).toOption,
      util.Try(receipt.getGasUsed.longValue()).toOption,
      Option(receipt.getEffectiveGasPrice).flatMap(hexStringToLong),
      receipt.getContractAddress,
      hexStringToLong(receipt.getStatus).get,
      receipt.getRoot,
      receipt.getLogs.asScala.map(x=> {
        val topics = x.getTopics.asScala.toArray
        `type`.Log(
          x.getLogIndex.longValue.toInt,
          x.getAddress,
          x.getData,
          getOrElse(topics, 0, null),
          getOrElse(topics, 1, null),
          getOrElse(topics, 2, null),
          getOrElse(topics, 3, null),
          x.getTransactionHash,
          x.getTransactionIndex.toString.toInt,
          0,
          x.getBlockNumber.longValue(),
          x.getBlockHash
        )
      }
      ).toArray
    )
  }

  def web3Transaction2Transaction(transaction: org.web3j.protocol.core.methods.response.Transaction) = {
    `type`.Transaction(
      transaction.getHash,
      transaction.getNonce.longValue(),
      transaction.getTransactionIndex.intValue(),
      transaction.getFrom,
      transaction.getTo,
      transaction.getValue,
      transaction.getGasPrice.longValue(),
      transaction.getGas.longValue(),
      hexStringToLong(transaction.getType).getOrElse(0),
      util.Try(transaction.getMaxFeePerGas.longValue()).toOption,
      util.Try(transaction.getMaxPriorityFeePerGas.longValue()).toOption,
      transaction.getInput,
      transaction.getBlockNumber.longValue(),
      0,
      transaction.getBlockHash,
      null
    )
  }

  def web3Block2Block(block: org.web3j.protocol.core.methods.response.EthBlock.Block) = {
    `type`.Block(
      block.getNumber.longValue(),
      block.getTimestamp.longValue(),
      block.getHash,
      block.getParentHash,
      block.getNonce.longValue(),
      block.getSha3Uncles,
      block.getLogsBloom,
      block.getTransactionsRoot,
      block.getStateRoot,
      block.getReceiptsRoot,
      block.getMiner,
      block.getDifficulty,
      block.getTotalDifficulty,
      block.getExtraData,
      Option(block.getSize.longValue()),
      Option(block.getGasLimit.longValue()),
      Option(block.getGasUsed.longValue()),
      hexStringToLong(block.getBaseFeePerGasRaw),
      block.getTransactions.asScala.map(x=> web3Transaction2Transaction(x.asInstanceOf[org.web3j.protocol.core.methods.response.Transaction])).toArray
    )
  }
}
