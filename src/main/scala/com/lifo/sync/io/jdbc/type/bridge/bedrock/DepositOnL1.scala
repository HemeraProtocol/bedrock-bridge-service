package com.lifo.sync.io.jdbc.`type`.bridge.bedrock

import com.lifo.sync.io.jdbc.`type`.PostgresqlRow

case class DepositOnL1(
                        version: Int,
                        index: Long,
                        l1_block_number: Long,
                        l1_block_timestamp: Long,
                        amount: BigInt,
                        l1_transaction_hash: String,
                        l1_from_address: String,
                        l1_to_address: String,
                        from_address: String,
                        to_address: String,
                        l1_token_address: String,
                        l2_token_address: String,
                        extra_info: Map[String, Any]
                      ) extends PostgresqlRow {
  override def onConflictSQL(tableName: String): String = {
    s"""
       |ON CONFLICT (version, index) DO UPDATE
       |SET l1_block_number = EXCLUDED.l1_block_number,
       |    l1_block_hash = EXCLUDED.l1_block_hash,
       |    l1_block_timestamp = EXCLUDED.l1_block_timestamp,
       |    l1_transaction_hash = EXCLUDED.l1_transaction_hash,
       |    amount = EXCLUDED.amount,
       |    l1_from_address = EXCLUDED.l1_from_address,
       |    l1_to_address = EXCLUDED.l1_to_address,
       |    from_address = EXCLUDED.from_address,
       |    to_address = EXCLUDED.to_address,
       |    l1_token_address = EXCLUDED.l1_token_address,
       |    l2_token_address = EXCLUDED.l2_token_address,
       |    extra_info = EXCLUDED.extra_info
       |""".stripMargin
  }

  override def tableName: String = "l1_to_l2_txns"
}
