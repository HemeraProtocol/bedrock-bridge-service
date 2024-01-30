package com.lifo.sync

import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.extractor.bridge.bedrock._
import com.lifo.sync.pipieline.ExportBridgePipeline
import com.lifo.sync.pipieline.filter.TransactionFilter
import com.lifo.sync.pipieline.filter.bridge.bedrock._
import scopt.OParser

object EthereumSync {

  def getFilterAndExtractor(config: EthereumSyncConfig): (TransactionFilter, BridgeExtractor) = {
    config.bridgeOnChain.get match {
      case BridgeOnChain.L1 =>
        (new BedRockBridgeTransactionFilterOnL1(config.contractAddress, config.web3_providers.head), BedrockBridgeL1Extractor)
      case BridgeOnChain.L2 =>
        (new BedRockBridgeTransactionFilterOnL2(config.contractAddress, config.web3_providers.head), BedrockBridgeL2Extractor)
      case BridgeOnChain.STATE_BATCH =>
        (new BedrockStateBatchesFilterOnL1(config.contractAddress, config.web3_providers.head), BedrockBatchesExtractor)
      case _ =>
        throw new Exception("Invalid bridge on chain")
    }
  }

  def main(args: Array[String]): Unit = {
    val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
    val builder = OParser.builder[EthereumSyncConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        // option -p, --web3_providers
        opt[Seq[String]]('p', "web3_providers")
          .valueName("<web3_provider1>,<web3_provider2>...")
          .action((x, c) => c.copy(web3_providers = x))
          .text("web3_providers is a list of web3 providers to connect with"),
        // option -d, --debug_web3_providers
        opt[Seq[String]]('d', "debug_web3_providers")
          .valueName("<debug_web3_provider1>,<debug_web3_provider2>...")
          .action((x, c) => c.copy(debug_web3_providers = x))
          .text("debug_web3_providers is a list of web3 providers to connect with for debug data"),
        // option --sync_file
        opt[String]("sync_file")
          .valueName("<file>")
          .action((x, c) => c.copy(sync_file = x))
          .text("sync_file is a file to store up to which block is synchronized"),
        // option --start
        opt[Long]('s', "start")
          .valueName("<start>")
          .action((x, c) => c.copy(start = Some(x)))
          .text("start block to begin synchronization from"),
        // option --end
        opt[Long]('e', "end")
          .valueName("<end>")
          .action((x, c) => c.copy(end = Some(x)))
          .text("end block to end synchronization at"),
        // option --batch_size
        opt[Int]('B', "batch_size")
          .action((x, c) => c.copy(batch_size = x))
          .text("batch_size is the number of blocks to synchronize in one batch"),
        // option --rpc_batch_size
        opt[Int]('b', "rpc_batch_size")
          .action((x, c) => c.copy(rpc_batch_size = x))
          .text("rpc_batch_size is the number of blocks to request from the rpc in one batch"),
        // option --rpc_batch_size
        opt[Int]("debug_rpc_batch_size")
          .action((x, c) => c.copy(debug_rpc_batch_size = x))
          .text("debug_rpc_batch_size is the number of blocks to request from the rpc in one batch with debug endpoints"),
        // option --parallelism
        opt[Int]('P', "parallelism")
          .action((x, c) => c.copy(parallelism = x))
          .text("parallelism is the number of threads to use for synchronization"),
        opt[String]("bridge_on_chain")
          .valueName("l1/l2/batch")
          .action((x, c) => c.copy(bridgeOnChain = Some(BridgeOnChain.withName(x.toUpperCase))))
          .text("bridgeOnChain is the chain on which the bridge is running. Possible values: l1, l2"),
        opt[String]("contract_address")
          .valueName("<contractAddress>")
          .action((x, c) => c.copy(contractAddress = x))
          .text("contractAddress is the address of the bridge contract")
      )
    }
    // OParser.parse returns Option[Config]
    OParser.parse(parser, args, EthereumSyncConfig()) match {
      case Some(config) =>
        // do something
        val (filter, extractor) = getFilterAndExtractor(config)

        val pipeline = new ExportBridgePipeline(
          config.web3_providers,
          config.start,
          config.sync_file,
          config.batch_size,
          config.rpc_batch_size,
          config.parallelism,
          filter,
          extractor
        )

        try {
          if (config.end.isDefined) {
            val t0 = System.currentTimeMillis()
            pipeline.start(config.end.get)
            val t1 = System.currentTimeMillis()
            logger.info(s"Synchronization complete in ${(t1 - t0) / 1000} seconds")
          } else {
            pipeline.startStreaming()
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
            System.exit(-1)
        }
        System.exit(0)
      case _ =>
        // arguments are bad, error message will have been displayed
        logger.error("Error parsing arguments")
    }
  }
}
