package com.lifo.sync.exporters

import java.util.concurrent.Executors

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

trait BatchedConcurrentExporter[T, R] {
  val concurrence: Int
  val batchSize: Int
  val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)
  val web3Providers: Seq[String]

  val threadPool = Executors.newFixedThreadPool(concurrence)
  implicit val customExecutionContext = ExecutionContext.fromExecutor(threadPool)

  def getRandomWeb3Provider: String = {
    scala.util.Random.shuffle(web3Providers).head
  }

  implicit val classTagT: ClassTag[T]
  implicit val classTagR: ClassTag[R]


  def export(data: Array[T]): Array[R]

  def exportAll(data: Array[T]): Array[R] = {
    val allBatches = data.grouped(batchSize).toList
    val groupedBatches = allBatches.grouped(concurrence)

    var processedBatches = 0
    val startTime = System.currentTimeMillis()
    logger.info(s"The data size is ${data.length}, starting to process ${allBatches.size} batches with concurrency $concurrence.")
    val allResults = groupedBatches.flatMap { concurrentBatches =>
      val futures = concurrentBatches.map { batch =>
        Future {
          val result = export(batch)
          processedBatches += 1
          logger.debug(s"Processed ${processedBatches} out of ${allBatches.size} batches.")
          result
        }
      }
      Await.result(Future.sequence(futures), Duration.Inf).flatten
    }.toArray
    val endTime = System.currentTimeMillis()
    val totalTime = (endTime - startTime) / 1000.0
    logger.info(f"All batches processed. Total time taken: $totalTime%.3f seconds.")
    allResults
  }
}
