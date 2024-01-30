package com.lifo.sync.`type`

case class Receipt(
                    transactionHash: String,
                    transactionIndex: Int,
                    cumulativeGasUsed: Option[Long],
                    gasUsed: Option[Long],
                    effectiveGasPrice: Option[Long],
                    contractAddress: String,
                    status: Long,
                    root: String,
                    logs: Array[Log]
                  )

