package com.lifo.sync.abi

import org.web3j.abi.datatypes.Type

case class Param(internalType: String, name: String, `type`: String, indexed: Option[Boolean], components: Option[List[Param]]) {

  def getSolidityType: Class[_ <: Type[_]] = `type` match {
    case "address" => classOf[org.web3j.abi.datatypes.Address]
    case "bool" => classOf[org.web3j.abi.datatypes.Bool]
    case "uint256" => classOf[org.web3j.abi.datatypes.generated.Uint256]
    case "uint128" => classOf[org.web3j.abi.datatypes.generated.Uint128]
    case "uint64" => classOf[org.web3j.abi.datatypes.generated.Uint64]
    case "uint32" => classOf[org.web3j.abi.datatypes.generated.Uint32]
    case "uint8" => classOf[org.web3j.abi.datatypes.generated.Uint8]
    case "string" => classOf[org.web3j.abi.datatypes.Utf8String]
    case "bytes32" => classOf[org.web3j.abi.datatypes.generated.Bytes32]
    case "tuple" => classOf[org.web3j.abi.datatypes.DynamicStruct]
    case "bytes" => classOf[org.web3j.abi.datatypes.DynamicBytes]
    case "bytes[]" => classOf[org.web3j.abi.datatypes.DynamicArray[_]]
    case "address[]" => classOf[org.web3j.abi.datatypes.DynamicArray[org.web3j.abi.datatypes.Address]]
    case _ => throw new IllegalArgumentException(s"Unknown Solidity type: ${`type`}")
  }
}