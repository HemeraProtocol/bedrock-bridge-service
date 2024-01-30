package com.lifo.sync.exporters

import com.lifo.sync.`type`.Receipt
import com.lifo.sync.rpc.Web3Rpc
import com.lifo.sync.transfer.Web3Block
import scala.reflect.ClassTag

class ReceiptExporter(val concurrence: Int, val web3Providers: Seq[String], val batchSize: Int)
  extends BatchedConcurrentExporter[String, Receipt] {

  implicit val classTagInt = implicitly[ClassTag[Int]]
  implicit val classTagT = implicitly[ClassTag[String]]
  implicit val classTagR = implicitly[ClassTag[Receipt]]

  def export(txnHashes: Array[String]): Array[Receipt] = {
    val service = new Web3Rpc(getRandomWeb3Provider)
    val receipts = service.getTransactionReceipts(txnHashes)
      txnHashes.zip(receipts).map(x => {
        val (txnHash, receipt) = x
        if (receipt == null) {
          println(s"Receipt is null for txn: $txnHash")
          Receipt(txnHash, 0, None, None, None, null, -1, null, Array())
        } else {
          Web3Block.web3Receipt2Receipt(receipt)
        }
      }).filter(_ != null)
  }
}
