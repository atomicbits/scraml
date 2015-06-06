package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.{JsArray, JsValue, JsObject}

import scala.language.postfixOps

/**
 * Created by peter on 5/06/15, Atomic BITS (http://atomicbits.io). 
 */
trait Schema {

  def id: Id

}

object Schema {

  def apply(schema: JsObject): Schema = {
    (schema \ "type").asOpt[String] match {
      case Some("object") =>
      case Some("array") =>
      case Some("string") =>
      case Some("number") =>
      case Some("integer") =>
      case Some("boolean") =>
      case Some("null") =>
      case None =>
        (schema \ "$ref").asOpt[String] match {
          case Some(_) => SchemaReference(schema)
          case None => Fragment(schema)
        }
    }
  }

  def updateLookup(lookup: SchemaLookup, schema: Schema): SchemaLookup = ???

  def simplify(schemas: List[Schema], lookup: SchemaLookup): List[Schema] = ???

}


case class Fragment(id: Id, fragments: Map[String, Schema]) extends Schema

object Fragment {

  def apply(schema: JsObject): Fragment = {
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }
    val fragments = schema.value.toSeq collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Schema(fragment))
    }
    Fragment(id, fragments.toMap)
  }

}

case class SchemaReference(id: Id, refersTo: Id) extends Schema

object SchemaReference {

  def apply(schema: JsObject): SchemaReference = {
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }
    val ref = schema match {
      case RefExtractor(refId) => refId
    }
    SchemaReference(id, ref)
  }

}

case class ObjectReference(id: Id, refersTo: ObjectEl) extends Schema

case class ObjectEl(id: Id,
                    properties: Map[String, Schema],
                    required: Map[String, Boolean] = Map.empty,
                    oneOfSelection: List[Selection] = List.empty,
                    fragments: Map[String, Schema] = Map.empty,
                    name: Option[String] = None,
                    canonicalName: Option[String] = None) extends Schema
// ToDo: handle the required field
// ToDo: handle the oneOf field
// ToDo: handle the name field
object ObjectEl {

  def apply(schema: JsObject): ObjectEl = {
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }
    val properties =
      schema \ "properties" match {
        case props: JsObject =>
          Some(props.value.toSeq collect {
            case (fieldName, fragment: JsObject) => (fieldName, Schema(fragment))
          } toMap)
        case _ => None
      }
    val keysToExclude = Seq("id", "properties", "required", "oneOf", "anyOf", "allOf")
    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](schema.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }
    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Schema(fragment))
    }
    ObjectEl(
      id = id,
      properties = properties.getOrElse(Map.empty[String, Schema]),
      fragments = fragments.toMap
    )
  }

}

case class ArrayEl(id: Id, items: Schema) extends Schema

case class StringEl(id: Id) extends Schema

case class NumberEl(id: Id) extends Schema

case class IntEl(id: Id) extends Schema

case class BooleanEl(id: Id) extends Schema

case class NullEl(id: Id) extends Schema

case class EnumEl(id: Id, choices: List[String]) extends Schema

trait Selection extends Schema

case class OneOf(id: Id,
                 selection: List[Schema],
                 discriminatorField: Option[String] = None) extends Selection

case class AnyOf(id: Id, selection: List[Schema]) extends Selection

case class AllOf(id: Id, selection: List[Schema]) extends Selection
