package com.lifo.sync.extractor.bridge.bedrock

import com.lifo.sync.`type`.Transaction
import com.lifo.sync.abi.AbiContract
import com.lifo.sync.extractor.bridge.BridgeExtractor
import com.lifo.sync.io.jdbc.`type`.PostgresqlRow
import com.lifo.sync.io.jdbc.`type`.bridge.bedrock.{DepositRelayOnL2, WithdrawalOnL2}
import com.lifo.sync.utils.Utils.{bytesToHex, getVersionAndIndexFromNonce}
import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer

object BedrockBridgeL2Extractor extends BridgeExtractor {

  private val function = com.lifo.sync.abi.Function("""{"inputs":[{"internalType":"uint256","name":"_nonce","type":"uint256"},{"internalType":"address","name":"_sender","type":"address"},{"internalType":"address","name":"_target","type":"address"},{"internalType":"uint256","name":"_value","type":"uint256"},{"internalType":"uint256","name":"_minGasLimit","type":"uint256"},{"internalType":"bytes","name":"_message","type":"bytes"}],"name":"relayMessage","outputs":[],"stateMutability":"payable","type":"function"}""")

  private val source = scala.io.Source.fromResource("contracts/op_l1_stand_bridge.json")
  private val rawJson = try source.mkString finally source.close()

  private val contract = AbiContract(rawJson)

  override def extract(transactions: Array[Transaction], contractAddress: String): ListBuffer[Array[PostgresqlRow]] = {
    val l2ToL1TransactionDeposit = transactions.filter(_.receipt.logs.exists(x => (x.topic0 == sentMessageEvent.signature && x.address == contractAddress)))
      .flatMap(x => parseL1withdrawalOnL2(x, contractAddress))
    val l1ToL2TransactionRelayMessage = transactions.filter(_.receipt.logs.exists(_.topic0 == relayedMessageEvent.signature))
      .map(parseL1DepositReceiptOnL2)
    ListBuffer(l2ToL1TransactionDeposit.asInstanceOf[Array[PostgresqlRow]], l1ToL2TransactionRelayMessage.asInstanceOf[Array[PostgresqlRow]])
  }

  def parseL1withdrawalOnL2(transaction: Transaction, contractAddress: String): Array[WithdrawalOnL2] = {

    val receipt = transaction.receipt
    receipt.logs.filter(x => x.topic0 == sentMessageEvent.signature && x.address == contractAddress).map(x => {
      val sentMessage = sentMessageEvent.decodeLogEvent(x).get
      val target = sentMessage("target")
      val sender = sentMessage("sender")
      val message = "0x" + bytesToHex(sentMessage("message").asInstanceOf[Array[Byte]])
      val nonce = BigInt(sentMessage("messageNonce").toString)
      val gasLimit = sentMessage("gasLimit").toString.toLong

      val (version, index) = getVersionAndIndexFromNonce(nonce)
      val info = contract.decodeInputRaw(message)
      val extraInfo = Map(
        "sender" -> sender.toString,
        "target" -> target.toString,
        "gas_limit" -> gasLimit,
      )

      if (info.isDefined) {
        val (functionName, input) = (info.get)
        functionName match {
          case "finalizeBridgeETH" =>
            WithdrawalOnL2(
              version = version,
              index = index,
              l1_token_address = "0x0000000000000000000000000000000000000000",
              l2_token_address = "0xdeaddeaddeaddeaddeaddeaddeaddeaddead1111",
              from_address = input("_from").toString,
              to_address = input("_to").toString,
              amount = BigInt(input("_amount").toString),
              l2_transaction_hash = receipt.transactionHash,
              l2_from_address = transaction.fromAddress,
              l2_to_address = transaction.toAddress,
              extra_info = extraInfo,
              l2_block_number = transaction.blockNumber,
              l2_block_timestamp = transaction.blockTimestamp,
              withdrawal_hash = null,
            )
          case "finalizeBridgeERC20" => WithdrawalOnL2(
            version = version,
            index = index,
            l1_token_address = input("_localToken").toString,
            l2_token_address = input("_remoteToken").toString,
            from_address = input("_from").toString,
            to_address = input("_to").toString,
            amount = BigInt(input("_amount").toString),
            l2_transaction_hash = receipt.transactionHash,
            l2_from_address = transaction.fromAddress,
            l2_to_address = transaction.toAddress,
            extra_info = extraInfo,
            l2_block_number = transaction.blockNumber,
            l2_block_timestamp = transaction.blockTimestamp,
            withdrawal_hash = null,
          )
          case _ => throw new Exception("Unsupported function info")
        }
      } else {
        WithdrawalOnL2(
          version = version,
          index = index,
          l1_token_address = null,
          l2_token_address = null,
          from_address = null,
          to_address = null,
          amount = 0,
          l2_transaction_hash = receipt.transactionHash,
          l2_from_address = transaction.fromAddress,
          l2_to_address = transaction.toAddress,
          extra_info = extraInfo + ("msg" -> message),
          l2_block_number = transaction.blockNumber,
          l2_block_timestamp = transaction.blockTimestamp,
          withdrawal_hash = null,
        )
      }
    }
    )
  }

  def parseL1DepositReceiptOnL2(transaction: Transaction): DepositRelayOnL2 = {
    val input = function.decodeInputRaw(transaction.input)
    val (version, index) = getVersionAndIndexFromNonce(BigInt(input("_nonce").toString))
    DepositRelayOnL2(
      version = version,
      index = index,
      l2_transaction_hash = transaction.hash,
      l2_block_number = transaction.blockNumber,
      l2_from_address = transaction.fromAddress,
      l2_to_address = transaction.toAddress,
      l2_block_timestamp = transaction.blockTimestamp,
    )
  }
}
