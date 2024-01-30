package com.lifo.sync

import com.lifo.sync.BridgeOnChain.BridgeOnChain

object BridgeOnChain extends Enumeration {
  type BridgeOnChain = Value
  val L1, L2, STATE_BATCH = Value
}

case class EthereumSyncConfig(
                               web3_providers: Seq[String] = Seq(),
                               debug_web3_providers: Seq[String] = Seq(),
                               sync_file: String = "./sync.txt",
                               start: Option[Long] = None,
                               end: Option[Long] = None,
                               batch_size: Int = 100,
                               rpc_batch_size: Int = 100,
                               debug_rpc_batch_size: Int = 20,
                               bridgeOnChain: Option[BridgeOnChain] = None,
                               parallelism: Int = 1,
                               contractAddress: String = null,
                             )
