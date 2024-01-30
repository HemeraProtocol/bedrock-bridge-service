package com.lifo.sync.`type`

case class Block(
                  number: Long,
                  timestamp: Long,
                  hash: String,
                  parentHash: String,
                  nonce: Long,
                  sha3Uncles: String,
                  logsBloom: String,
                  transactionsRoot: String,
                  stateRoot: String,
                  receiptsRoot: String,
                  miner: String,
                  difficulty: BigInt,
                  totalDifficulty: BigInt,
                  extraData: String,
                  size: Option[Long],
                  gasLimit: Option[Long],
                  gasUsed: Option[Long],
                  baseFeePerGas: Option[Long],
                  transactions: Array[Transaction]
                )