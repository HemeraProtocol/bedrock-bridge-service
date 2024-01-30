package com.lifo.sync.exporters

import com.lifo.sync.`type`.Block
import com.lifo.sync.rpc.Web3Rpc
import com.lifo.sync.transfer.Web3Block.web3Block2Block
import scala.reflect.ClassTag

class BlockExporter(
                     val concurrence: Int, val web3Providers: Seq[String], val batchSize: Int
                   )
  extends BatchedConcurrentExporter[Long, Block] {

  implicit val classTagInt = implicitly[ClassTag[Int]]
  implicit val classTagT = implicitly[ClassTag[Long]]
  implicit val classTagR = implicitly[ClassTag[Block]]

  def export(blockNumbers: Array[Long]): Array[Block] = {
    logger.debug("Exporting blocks size " + blockNumbers.size)
    val service = new Web3Rpc(getRandomWeb3Provider)
    val blocks = service.getBlocks(blockNumbers).toArray.map(web3Block2Block)
    logger.debug("Exported blocks size " + blockNumbers.size)
    blocks
  }

  def exportAll(start: Long, end: Long): Array[Block] = {
    val blockNumbers = (start + 1 to end).toArray
    super.exportAll(blockNumbers)
  }
}
