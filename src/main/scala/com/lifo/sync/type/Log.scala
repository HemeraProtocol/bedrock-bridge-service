package com.lifo.sync.`type`

import scala.collection.JavaConverters._

case class Log(
                logIndex: Int,
                address: String,
                data: String,
                topic0: String = null,
                topic1: String = null,
                topic2: String = null,
                topic3: String = null,
                transactionHash: String,
                transactionIndex: Int,
                var blockTimestamp: Long,
                blockNumber: Long,
                blockHash: String
              ) {
  def toWeb3Log = {
    new org.web3j.protocol.core.methods.response.Log(
      false,
      logIndex.toString,
      transactionIndex.toString,
      transactionHash,
      blockHash,
      blockNumber.toString,
      address,
      data,
      null,
      List(topic0, topic1, topic2, topic3).filter(_!=null).asJava
    )
  }
}
