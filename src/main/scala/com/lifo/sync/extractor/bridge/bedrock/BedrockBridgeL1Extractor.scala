package com.lifo.sync.extractor.bridge.bedrock

import com.lifo.sync.`type`.Transaction
import com.lifo.sync.abi.AbiContract
import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.io.jdbc.`type`.PostgresqlRow
import com.lifo.sync.io.jdbc.`type`.bridge.bedrock.{DepositOnL1, WithdrawalFinalizedOnL1, WithdrawalProvenOnL1}
import com.lifo.sync.utils.Utils.{bytesToHex, getVersionAndIndexFromNonce}
import scala.collection.mutable.ListBuffer

object BedrockBridgeL1Extractor extends BridgeExtractor {

  private val source = scala.io.Source.fromResource("contracts/op_l1_stand_bridge.json")
  private val rawJson = try source.mkString finally source.close()

  private val contract = AbiContract(rawJson)

  override def extract(transactions: Array[Transaction]): ListBuffer[Array[PostgresqlRow]] = {
    val l1ToL2TransactionDeposit = transactions.filter(_.receipt.logs.exists(_.topic0 == transactionDepositedEvent.signature))
      .map(parseL1DepositReceipt)
    val l2ToL1TransactionWithdrawalProve = transactions.filter(_.receipt.logs.exists(_.topic0 == withdrawalProvenEvent.signature))
      .map(parseL1WithdrawalProvenReceipt)
    val l2ToL1TransactionWithdrawalFinalize = transactions.filter(_.receipt.logs.exists(_.topic0 == withdrawalFinalizedEvent.signature))
      .map(parseL1WithdrawalFinalizedReceipt)
    ListBuffer(
      l1ToL2TransactionDeposit.asInstanceOf[Array[PostgresqlRow]],
      l2ToL1TransactionWithdrawalProve.asInstanceOf[Array[PostgresqlRow]],
      l2ToL1TransactionWithdrawalFinalize.asInstanceOf[Array[PostgresqlRow]],
    )
  }

  def parseL1DepositReceipt(transaction: Transaction): DepositOnL1 = {

    val receipt = transaction.receipt
    val transactionDeposited = transactionDepositedEvent.decodeLogEvent(receipt.logs.filter(_.topic0 == transactionDepositedEvent.signature).head).get
    val tx_origin = transactionDeposited("from")

    val sentMessage = sentMessageEvent.decodeLogEvent(receipt.logs.filter(_.topic0 == sentMessageEvent.signature).head).get

    val target = sentMessage("target")
    val sender = sentMessage("sender")
    val message = "0x" + bytesToHex(sentMessage("message").asInstanceOf[Array[Byte]])
    val nonce = BigInt(sentMessage("messageNonce").toString)
    val gasLimit = sentMessage("gasLimit").toString.toLong

    val info = contract.decodeInputRaw(message)
    val extraInfo = Map(
      "sender" -> sender,
      "target" -> target,
      "tx_origin" -> tx_origin,
      "gas_limit" -> gasLimit,
    )
    val (version, index) = getVersionAndIndexFromNonce(nonce)
    if (info.isDefined) {
      val (functionName, input) = (info.get)
      functionName match {
        case "finalizeBridgeETH" =>
          DepositOnL1(
            version = version,
            index = index,
            l1_token_address = "0x0000000000000000000000000000000000000000",
            l2_token_address = "0xdeaddeaddeaddeaddeaddeaddeaddeaddead1111",
            from_address = input("_from").toString,
            to_address = input("_to").toString,
            amount = BigInt(input("_amount").toString),
            l1_transaction_hash = receipt.transactionHash,
            extra_info = extraInfo,
            l1_from_address = transaction.fromAddress,
            l1_to_address = transaction.toAddress,
            l1_block_number = transaction.blockNumber,
            l1_block_timestamp = transaction.blockTimestamp,
          )
        case "finalizeBridgeERC20" => DepositOnL1(
          version = version,
          index = index,
          l1_token_address = input("_remoteToken").toString,
          l2_token_address = input("_localToken").toString,
          from_address = input("_from").toString,
          to_address = input("_to").toString,
          amount = BigInt(input("_amount").toString),
          l1_transaction_hash = receipt.transactionHash,
          extra_info = extraInfo,
          l1_from_address = transaction.fromAddress,
          l1_to_address = transaction.toAddress,
          l1_block_number = transaction.blockNumber,
          l1_block_timestamp = transaction.blockTimestamp,
        )
        case _ => throw new Exception("Unsupported function info")
      }
    } else {
      DepositOnL1(
        version = version,
        index = index,
        l1_token_address = null,
        l2_token_address = null,
        from_address = null,
        to_address = null,
        amount = 0,
        l1_transaction_hash = receipt.transactionHash,
        extra_info = extraInfo + ("msg" -> message),
        l1_from_address = transaction.fromAddress,
        l1_to_address = transaction.toAddress,
        l1_block_number = transaction.blockNumber,
        l1_block_timestamp = transaction.blockTimestamp,
      )
    }
  }

  def parseL1WithdrawalProvenReceipt(transaction: Transaction): WithdrawalProvenOnL1 = {
    val nonce = BigInt(
      proveWithdrawalTransactionFunction.decodeInputRaw(transaction.input)("_tx").asInstanceOf[Map[String, Any]]("nonce").toString
    )
    val (version, index) = getVersionAndIndexFromNonce(nonce)
    val receipt = transaction.receipt
    val withdrawalProven = withdrawalProvenEvent.decodeLogEvent(receipt.logs.filter(_.topic0 == withdrawalProvenEvent.signature).head).get
    val withdrawalHash = bytesToHex(withdrawalProven("withdrawalHash").asInstanceOf[Array[Byte]])

    WithdrawalProvenOnL1(
      version = version,
      index = index,
      withdrawalHash,
      receipt.transactionHash,
      l1_proven_from_address= transaction.fromAddress,
      l1_proven_to_address = transaction.toAddress,
      l1_proven_block_number = transaction.blockNumber,
      l1_proven_block_timestamp = transaction.blockTimestamp,
    )
  }

  def parseL1WithdrawalFinalizedReceipt(transaction: Transaction): WithdrawalFinalizedOnL1 = {
    val nonce = BigInt(
      finalizeWithdrawalTransactionFunction.decodeInputRaw(transaction.input)("_tx").asInstanceOf[Map[String, Any]]("nonce").toString
    )
    val receipt = transaction.receipt
    val withdrawalFinalized = withdrawalFinalizedEvent.decodeLogEvent(receipt.logs.filter(_.topic0 == withdrawalFinalizedEvent.signature).head).get
    val withdrawalHash = bytesToHex(withdrawalFinalized("withdrawalHash").asInstanceOf[Array[Byte]])

    val (version, index) = getVersionAndIndexFromNonce(nonce)
    WithdrawalFinalizedOnL1(
      version = version,
      index = index,
      withdrawalHash,
      receipt.transactionHash,
      l1_finalized_from_address = transaction.fromAddress,
      l1_finalized_to_address = transaction.toAddress,
      l1_finalized_block_number = transaction.blockNumber,
      l1_finalized_block_timestamp = transaction.blockTimestamp,
    )
  }
}
