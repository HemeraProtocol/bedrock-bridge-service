package com.lifo.sync.extractor.bridge

import com.lifo.sync.`type`.Transaction
import com.lifo.sync.io.jdbc.`type`.PostgresqlRow
import scala.collection.mutable.ListBuffer

trait BridgeExtractor {
    def extract(transactions: Array[Transaction], contractAddress: String): ListBuffer[Array[PostgresqlRow]] = ???
}
