package com.lifo.sync.extractor.bridge.bedrock

import com.lifo.sync.`type`.Transaction
import com.lifo.sync.abi.AbiContract
import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.io.jdbc.`type`.PostgresqlRow
import com.lifo.sync.io.jdbc.`type`.bridge.bedrock.{DepositOnL1, WithdrawalFinalizedOnL1, WithdrawalProvenOnL1}
import com.lifo.sync.utils.Utils.{bytesToHex, getVersionAndIndexFromNonce}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Try}

object BedrockBridgeL1Extractor extends BridgeExtractor {
  private val function = com.lifo.sync.abi.Function("""{"inputs":[{"internalType":"uint256","name":"_nonce","type":"uint256"},{"internalType":"address","name":"_sender","type":"address"},{"internalType":"address","name":"_target","type":"address"},{"internalType":"uint256","name":"_value","type":"uint256"},{"internalType":"uint256","name":"_minGasLimit","type":"uint256"},{"internalType":"bytes","name":"_message","type":"bytes"}],"name":"relayMessage","outputs":[],"stateMutability":"payable","type":"function"}""")

  private val source = scala.io.Source.fromResource("contracts/op_l1_stand_bridge.json")
  private val rawJson = try source.mkString finally source.close()
  private val contract = AbiContract(rawJson)

  def unmarshalDepositVersion0(opaqueData: Array[Byte]): Try[(Option[BigInt], BigInt, BigInt, Boolean, Array[Byte])] = {
    if (opaqueData.length < 32 + 32 + 8 + 1) {
      Failure(new Exception(s"Unexpected opaqueData length: ${opaqueData.length}"))
    } else {
      Try {
        var offset = 0

        // uint256 mint
        val mint = BigInt(opaqueData.slice(offset, offset + 32))
        val mintOption = if (mint == BigInt(0)) None else Some(mint)
        offset += 32

        // uint256 value
        val value = BigInt(opaqueData.slice(offset, offset + 32))
        offset += 32

        // uint64 gas
        val gas = BigInt(1, opaqueData.slice(offset, offset + 8))
        if (gas.bitLength > 64) {
          throw new Exception(s"Bad gas value: ${opaqueData.slice(offset, offset + 8).mkString}")
        }
        offset += 8

        // uint8 isCreation
        val isCreation = opaqueData(offset) == 1
        offset += 1

        // transaction data
        val txData = opaqueData.slice(offset, opaqueData.length)

        (mintOption, value, gas, isCreation, txData)
      }
    }
  }

  override def extract(transactions: Array[Transaction], contractAddress: String): ListBuffer[Array[PostgresqlRow]] = {
    val l1ToL2TransactionDeposit = transactions
      .filter(_.receipt.logs.exists(x => x.topic0 == transactionDepositedEvent.signature && x.address == contractAddress))
      .flatMap(x => parseL1DepositReceipt(x, contractAddress))
    val l2ToL1TransactionWithdrawalProve = transactions
      .filter(_.receipt.logs.exists(x => x.topic0 == withdrawalProvenEvent.signature && x.address == contractAddress))
      .map(x => parseL1WithdrawalProvenReceipt(x, contractAddress))
    val l2ToL1TransactionWithdrawalFinalize = transactions
      .filter(_.receipt.logs.exists(x => x.topic0 == withdrawalFinalizedEvent.signature && x.address == contractAddress))
      .map(x => parseL1WithdrawalFinalizedReceipt(x, contractAddress))
    ListBuffer(
      l1ToL2TransactionDeposit.asInstanceOf[Array[PostgresqlRow]],
      l2ToL1TransactionWithdrawalProve.asInstanceOf[Array[PostgresqlRow]],
      l2ToL1TransactionWithdrawalFinalize.asInstanceOf[Array[PostgresqlRow]],
    )
  }

  def parseL1DepositReceipt(transaction: Transaction, contractAddress: String): Array[DepositOnL1] = {

    val receipt = transaction.receipt

    receipt.logs.filter(
      x => x.topic0 == transactionDepositedEvent.signature && x.address == contractAddress
    ).flatMap(x => {
      val transactionDeposited = transactionDepositedEvent.decodeLogEvent(x).get
      val tx_origin = transactionDeposited("from")
      val (_, _, _, _, txData) = unmarshalDepositVersion0(transactionDeposited("opaqueData").asInstanceOf[Array[Byte]]).getOrElse((None, 0, 0, false, Array.emptyByteArray))
      if (txData.length == 0) {
        None
      } else {
        val tx = function.decodeInputRaw("0x" + bytesToHex(txData))
        val target = tx("_target")
        val sender = tx("_sender")
        val message = "0x" + bytesToHex(tx("_message").asInstanceOf[Array[Byte]])
        val nonce = BigInt(tx("_nonce").toString)
        val gasLimit = tx("_minGasLimit").toString.toLong

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
              Some(DepositOnL1(
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
              ))
            case "finalizeBridgeERC20" => Some(DepositOnL1(
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
            ))
            case _ => throw new Exception("Unsupported function info")
          }
        } else {
          Some(DepositOnL1(
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
          ))
        }
      }

    }
    )
  }

  def parseL1WithdrawalProvenReceipt(transaction: Transaction, contractAddress: String): WithdrawalProvenOnL1 = {
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
      l1_proven_from_address = transaction.fromAddress,
      l1_proven_to_address = transaction.toAddress,
      l1_proven_block_number = transaction.blockNumber,
      l1_proven_block_timestamp = transaction.blockTimestamp,
    )
  }

  def parseL1WithdrawalFinalizedReceipt(transaction: Transaction, contractAddress: String): WithdrawalFinalizedOnL1 = {
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
