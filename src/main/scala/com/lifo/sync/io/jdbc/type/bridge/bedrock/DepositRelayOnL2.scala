package com.lifo.sync.io.jdbc.`type`.bridge.bedrock

import com.lifo.sync.io.jdbc.`type`.PostgresqlRow

case class DepositRelayOnL2(
                             version: Int,
                             index: Long,
                             l2_block_number: Long,
                             l2_block_timestamp: Long,
                             l2_transaction_hash: String,
                             l2_from_address: String,
                             l2_to_address: String,
                           ) extends PostgresqlRow {
  override def onConflictSQL(tableName: String): String = {
    s"""
       |ON CONFLICT (version, index) DO UPDATE
       |SET l2_block_number = EXCLUDED.l2_block_number,
       |    l2_block_timestamp = EXCLUDED.l2_block_timestamp,
       |    l2_transaction_hash = EXCLUDED.l2_transaction_hash,
       |    l2_from_address = EXCLUDED.l2_from_address,
       |    l2_to_address = EXCLUDED.l2_to_address
       |""".stripMargin
  }

  override def tableName: String = "l1_to_l2_txns"
}
