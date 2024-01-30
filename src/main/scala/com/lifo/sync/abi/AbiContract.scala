package com.lifo.sync.abi

import org.json4s.native.JsonMethods.parse
import org.json4s._

object AbiContract {

  class ContractElementDeserializer extends CustomSerializer[ContractElement](format => ( {
    case jsonObj: JObject =>
      jsonObj \ "type" match {
        case JString("constructor") => jsonObj.extract[Constructor]
        case JString("event") => jsonObj.extract[Event]
        case JString("function") => jsonObj.extract[Function]
        case JString("receive") => jsonObj.extract[Constructor]
        case JString("error") => jsonObj.extract[Error]
        case _ => throw new MappingException(s"Unknown type in JSON: $jsonObj")
      }
  }, {
    case constructor: Constructor => Extraction.decompose(constructor)(formats)
    case event: Event => Extraction.decompose(event)(formats)
    case function: Function => Extraction.decompose(function)(formats)
  }))

  implicit val formats: Formats = DefaultFormats + new ContractElementDeserializer

  def apply(abiJson: String): AbiContract = new AbiContract(parse(abiJson).extract[Array[ContractElement]])
}

class AbiContract(abi: Array[ContractElement]) {

  def getEvents: Map[String, Event] = {
    abi.collect {
      case event: Event => event.name -> event
    }.toMap
  }

  def getFunctions: Map[String, Function] = {
    abi.collect {
      case function: Function => function.name -> function
    }.toMap
  }

  def decodeInputRaw(raw: String): Option[(String, Map[String, Any])] = {
    abi.collect {
      case function: Function => function.signature.substring(0, 10) -> function
    }.toMap.get(raw.substring(0, 10)).map(x => (x.name, x.decodeInputRaw(raw)))
  }
}
