package com.lifo.sync.pipieline.filter

import com.lifo.sync.`type`.TransactionInfo
import com.lifo.sync.abi.Event
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthLog.LogObject
import org.web3j.protocol.http.HttpService
import scala.collection.JavaConverters._

trait TransactionFilter {

  val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  val contractAddress: String
  val web3Provider: String
  val events: Array[Event]

  def rangeFilterByEvents(startBlock: Long, endBlock: Long): Array[TransactionInfo] = {
    val web3 = Web3j.build(new HttpService(web3Provider))
    val filterParams = new EthFilter(
      DefaultBlockParameter.valueOf(BigInt(startBlock).bigInteger),
      DefaultBlockParameter.valueOf(BigInt(endBlock).bigInteger),
      List(contractAddress).asJava
    ).addOptionalTopics(events.map(_.signature): _*)
    web3.ethGetLogs(filterParams).send().getLogs.asScala.map(_.asInstanceOf[LogObject]).map(
      x => {
        TransactionInfo(
          x.get().getTransactionHash,
          x.get().getTransactionIndex.intValue(),
          x.get().getBlockNumber.longValue(),
          x.get().getBlockHash,
        )
      }
    ).toArray
  }
}
