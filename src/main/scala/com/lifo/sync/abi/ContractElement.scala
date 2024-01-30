package com.lifo.sync.abi

import com.lifo.sync.`type`.Log
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.web3j.abi.datatypes.Type
import org.web3j.abi.TypeDecoder
import org.web3j.crypto.Hash

sealed trait ContractElement

case class Error(inputs: List[Param], name: String) extends ContractElement
case class Constructor(inputs: List[Param], stateMutability: String, `type`: String) extends ContractElement

object Event {
  implicit val formats: Formats = DefaultFormats + FieldSerializer[Param]()

  def apply(json: String): Event = {
    parse(json).extract[Event]
  }
}

case class Event(anonymous: Boolean, inputs: List[Param], name: String, `type`: String) extends ContractElement {

  def signature: String = {
    val inputTypes = inputs.map(_.`type`)
    val signature = s"$name(${inputTypes.mkString(",")})"
    Hash.sha3String(signature)
  }

  def decodeLogEventWithNoAssert(log: Log): Option[Map[String, Any]] = {
    val topics = Array(log.topic1, log.topic2, log.topic3).filter(_ != null)
    assert(topics.length == inputs.count(_.indexed.get))
    var map = scala.collection.mutable.Map.empty[String, Any]
    for ((param, index) <- inputs.filter(_.indexed.get).zipWithIndex) {
      val decodedValue = TypeDecoder.decode(topics(index), 2, param.getSolidityType).getValue
      map += (param.name -> decodedValue)
    }

    var data = log.data.substring(2)
    var offset = 0
    for (param <- inputs.filter(!_.indexed.get)) {
      var decodedValue: Any = null
      if (param.getSolidityType == classOf[org.web3j.abi.datatypes.DynamicBytes] || param.getSolidityType == classOf[org.web3j.abi.datatypes.Utf8String]) {
        val dynamicOffset = TypeDecoder.decode(data, offset, classOf[org.web3j.abi.datatypes.generated.Uint256]).getValue.intValue()
        decodedValue = TypeDecoder.decode(data, dynamicOffset * 2, param.getSolidityType).getValue
        offset += 64
      } else {
        val decoded = TypeDecoder.decode(data, offset, param.getSolidityType)
        offset += decoded.asInstanceOf[Type[_]].bytes32PaddedLength() * 2
        decodedValue = decoded.getValue
      }
      map += (param.name -> decodedValue)
    }
    Some(map.toMap)
  }
  def decodeLogEvent(log: Log): Option[Map[String, Any]] = {

    assert(signature == log.topic0)
    decodeLogEventWithNoAssert(log)
  }
}

object Function {
  implicit val formats: Formats = DefaultFormats + FieldSerializer[Param]()

  def apply(json: String): Function = {
    parse(json).extract[Function]
  }
}

case class Function(
                     inputs: List[Param],
                     name: String,
                     outputs: List[Param],
                     stateMutability: String,
                     `type`: String
                   ) extends ContractElement {
  def signature: String = {
    val inputTypes = inputs.map(x =>
      x.`type` match {
        case "tuple" => x.components.get.map(_.`type`).mkString("(", ",", ")")
        case _ => x.`type`
      }
    )
    val signature = s"$name(${inputTypes.mkString(",")})"
    Hash.sha3String(signature)
  }

  def decode(startOffset: Int, rawInput: String, inputs: List[Param]): Map[String, Any] = {
    var map = scala.collection.mutable.Map.empty[String, Any]
    var offset = startOffset

    for (param <- inputs) {
      var decodedValue: Any = null
      if (param.getSolidityType == classOf[org.web3j.abi.datatypes.DynamicBytes]) {
        val dynamicOffset = TypeDecoder.decode(rawInput, offset, classOf[org.web3j.abi.datatypes.generated.Uint256]).getValue.intValue()
        decodedValue = TypeDecoder.decode(rawInput, dynamicOffset * 2, param.getSolidityType).getValue
        offset += 64
      } else if (param.getSolidityType == classOf[org.web3j.abi.datatypes.DynamicStruct]) {
        val dynamicOffset = TypeDecoder.decode(rawInput, offset, classOf[org.web3j.abi.datatypes.generated.Uint256]).getValue.intValue()
        decodedValue = decode(dynamicOffset * 2, rawInput, param.components.get)
        offset += 64
      } else if (param.getSolidityType == classOf[org.web3j.abi.datatypes.DynamicArray[_]]){
        // TODO: support dynamic array
      } else {
        decodedValue = TypeDecoder.decode(rawInput, offset, param.getSolidityType).getValue
        offset += TypeDecoder.decode(rawInput, offset, param.getSolidityType).asInstanceOf[Type[_]].bytes32PaddedLength() * 2
      }
      map += (param.name -> decodedValue)
    }
    map.toMap
  }

  def decodeInputRaw(rawInput: String): Map[String, Any] = {
    if (signature.substring(0, 10) != rawInput.substring(0, 10)) {
      Map()
    } else {
      val data = rawInput.substring(10)
      decode(0, data, inputs)
    }
  }
}
