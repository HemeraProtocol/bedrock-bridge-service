package com.lifo.sync.io.jdbc.`type`.bridge.bedrock

import com.lifo.sync.io.jdbc.`type`.PostgresqlRow

case class StateBatchesOnL1(
                             batch_index: Long,
                             l1_block_number: Long,
                             l1_block_timestamp: Long,
                             l1_block_hash: String,
                             l1_transaction_hash: String,
                             end_block_number: Long,
                             batch_root: String,
                             var start_block_number: Long = 0,
                           ) extends PostgresqlRow {
  override def tableName: String = "op_bedrock_state_batches"

}
