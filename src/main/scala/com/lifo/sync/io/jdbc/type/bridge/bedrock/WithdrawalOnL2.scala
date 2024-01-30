package com.lifo.sync.io.jdbc.`type`.bridge.bedrock

import com.lifo.sync.io.jdbc.`type`.PostgresqlRow


case class WithdrawalOnL2(
                           version: Int,
                           index: Long,
                           l2_block_number: Long,
                           l2_block_timestamp: Long,
                           l2_transaction_hash: String,
                           l2_from_address: String,
                           l2_to_address: String,
                           amount: BigInt,
                           from_address: String,
                           to_address: String,
                           l1_token_address: String,
                           l2_token_address: String,
                           withdrawal_hash: String,
                           extra_info: Map[String, Any]
                         ) extends PostgresqlRow {
  override def onConflictSQL(tableName: String): String = {
    s"""
       |ON CONFLICT (version, index) DO UPDATE
       |SET l2_block_number = EXCLUDED.l2_block_number,
       |    l2_block_timestamp = EXCLUDED.l2_block_timestamp,
       |    l2_transaction_hash = EXCLUDED.l2_transaction_hash,
       |    l2_from_address = EXCLUDED.l2_from_address,
       |    amount = EXCLUDED.amount,
       |    l2_to_address = EXCLUDED.l2_to_address,
       |    from_address = EXCLUDED.from_address,
       |    to_address = EXCLUDED.to_address,
       |    l1_token_address = EXCLUDED.l1_token_address,
       |    l2_token_address = EXCLUDED.l2_token_address,
       |    extra_info = EXCLUDED.extra_info
       |""".stripMargin
  }

  override def tableName: String = "l2_to_l1_txns"
}
