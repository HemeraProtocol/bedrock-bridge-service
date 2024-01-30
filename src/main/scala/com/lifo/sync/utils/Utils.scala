package com.lifo.sync.utils

import scala.collection.mutable.ListBuffer
import scala.collection.Seq

object Utils {

  val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"


  def hexStringToUTF8(hex: String): String = {
    if (hex.startsWith("0x")) {
      hexStringToUTF8(hex.drop(2))
    } else {
      val byteArray = hex.sliding(2, 2).toArray.map(hexByte => Integer.parseInt(hexByte, 16).toByte)
      new String(byteArray, "UTF-8")
    }
  }

  def normalizeAddress(
                        l1TokenAddress: String
                      ): String = {
    if (l1TokenAddress == null || l1TokenAddress == ZERO_ADDRESS) {
      null
    } else {
      l1TokenAddress
    }
  }

  def getVersionAndIndexFromNonce(nonce: BigInt): (Int, Long) = {
    val version = (nonce >> 240).toInt
    (version, (nonce & BigInt("0000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)).toLong)
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    bytes.map("%02x".format(_)).mkString
  }

  def hexStringToLong(hex: String): Option[Long] = {
    if (hex == null) {
      None
    } else {
      val normalizedHex = if (hex.startsWith("0x")) hex.drop(2) else hex
      util.Try(java.lang.Long.parseLong(hex.substring(2), 16)).toOption
    }
  }

  def hexStringToBytes(hex: String): Array[Byte] = {
    if (hex == null) {
      return null
    }
    val normalizedHex =
      if (hex.startsWith("0x")) hex.drop(2)
      else hex
    util.Try(normalizedHex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)).getOrElse(null)
  }

  def splitToWords(data: String): Seq[String] = {
    if (data != null && data.length > 2) {
      val dataWithout0x = data.substring(2)
      val words = chunkString(dataWithout0x, 64)
      val wordsWith0x = words.map(word => "0x" + word)
      wordsWith0x
    } else {
      Seq.empty
    }
  }

  def toNormalizedAddress(address: String): String = {
    if (address == null || !address.isInstanceOf[String]) {
      address
    } else {
      address.toLowerCase
    }
  }

  def wordToAddress(param: String): String = {
    if (param == null) {
      null
    } else if (param.length >= 40) {
      toNormalizedAddress("0x" + param.takeRight(40))
    } else {
      toNormalizedAddress(param)
    }
  }

  def chunkString(data: String, chunkSize: Int): Seq[String] = {
    val chunks = new ListBuffer[String]()
    val length = data.length
    var i = 0
    while (i < length) {
      chunks += data.substring(i, Math.min(length, i + chunkSize))
      i += chunkSize
    }
    chunks.toList
  }
}
