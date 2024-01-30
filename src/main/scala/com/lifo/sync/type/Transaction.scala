package com.lifo.sync.`type`

case class Transaction(
                        hash: String,
                        nonce: Long,
                        transactionIndex: Int,
                        fromAddress: String,
                        toAddress: String,
                        value: BigInt,
                        gasPrice: Long,
                        gas: Long,
                        transactionType: Long,
                        maxFeePerGas: Option[Long],
                        maxPriorityFeePerGas: Option[Long],
                        input: String,
                        blockNumber: Long,
                        var blockTimestamp: Long,
                        blockHash: String,
                        var receipt: Receipt
                      )