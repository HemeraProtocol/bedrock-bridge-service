package com.lifo.sync.pipieline.filter.bridge.bedrock

import com.lifo.sync.abi.Event
import com.lifo.sync.extractor.bridge.bedrock.outputProposedEvent
import com.lifo.sync.pipieline.filter.TransactionFilter

class BedrockStateBatchesFilterOnL1(
                                     val contractAddress: String,
                                     val web3Provider: String
                                   ) extends TransactionFilter {
  override val events: Array[Event] = Array(
    outputProposedEvent
  )
}
