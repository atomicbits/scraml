package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json._

import scala.language.postfixOps

/**
 * Created by peter on 5/06/15, Atomic BITS (http://atomicbits.io).
 *
 * See: http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7
 *
 */
trait Schema {

  def id: Id

}

object Schema {

  def apply(schema: JsObject): Schema = {
    (schema \ "type").asOpt[String] match {
      case Some("object") => ObjectEl(schema)
      case Some("array") => ArrayEl(schema)
      case Some("string") => StringEl(schema)
      case Some("number") => NumberEl(schema)
      case Some("integer") => IntegerEl(schema)
      case Some("boolean") => BooleanEl(schema)
      case Some("null") => NullEl(schema)
      case None =>
        (schema \ "$ref").asOpt[String] match {
          case Some(_) => SchemaReference(schema)
          case None =>
            (schema \ "enum").asOpt[List[String]] match {
              case Some(choices) => EnumEl(schema)
              case None => Fragment(schema)
            }
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
                    required: Boolean,
                    requiredFields: List[String] = List.empty,
                    oneOfSelection: List[Selection] = List.empty,
                    fragments: Map[String, Schema] = Map.empty,
                    name: Option[String] = None,
                    canonicalName: Option[String] = None) extends Schema

// ToDo: handle the oneOf field
object ObjectEl {

  def apply(schema: JsObject): ObjectEl = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the properties
    val properties =
      schema \ "properties" match {
        case props: JsObject =>
          Some(props.value.toSeq collect {
            case (fieldName, fragment: JsObject) => (fieldName, Schema(fragment))
          } toMap)
        case _ => None
      }

    // Process the fragments
    val keysToExclude = Seq("id", "properties", "required", "oneOf", "anyOf", "allOf")
    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](schema.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }
    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Schema(fragment))
    }

    // Process the required field
    val (required, requiredFields) =
      schema \ "required" match {
        case req: JsArray =>
          (None, Some(req.value.toList collect {
            case JsString(value) => value
          }))
        case JsBoolean(b) => (Some(b), None)
        case _ => (None, None)
      }

    // Process the named field
    val name = (schema \ "name").asOpt[String]

    ObjectEl(
      id = id,
      required = required.getOrElse(false),
      requiredFields = requiredFields.getOrElse(List.empty[String]),
      properties = properties.getOrElse(Map.empty[String, Schema]),
      fragments = fragments.toMap,
      name = name
    )

  }

}


case class ArrayEl(id: Id, items: Schema, required: Boolean = false) extends Schema

object ArrayEl {

  def apply(schema: JsObject): ArrayEl = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the items type
    val items =
      schema \ "items" match {
        case obj: JsObject => Some(Schema(obj))
        case _ => None
      }

    // Process the required field
    val required = (schema \ "required").asOpt[Boolean]

    ArrayEl(
      id = id,
      items = items.getOrElse(
        throw JsonSchemaParseException("An array type must have an 'items' field that refers to a JsObject")
      ),
      required = required.getOrElse(false)
    )

  }

}


/**
 *
 * @param id
 * @param format See http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7
 * @param required
 */
case class StringEl(id: Id, format: Option[String] = None, required: Boolean = false) extends Schema

object StringEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val format = (schema \ "format").asOpt[String]

    val required = (schema \ "required").asOpt[Boolean]

    StringEl(id, format, required.getOrElse(false))

  }

}


case class NumberEl(id: Id, required: Boolean = false) extends Schema

object NumberEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val required = (schema \ "required").asOpt[Boolean]

    NumberEl(id, required.getOrElse(false))

  }

}


case class IntegerEl(id: Id, required: Boolean = false) extends Schema

object IntegerEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val required = (schema \ "required").asOpt[Boolean]

    IntegerEl(id, required.getOrElse(false))

  }

}


case class IntEl(id: Id, required: Boolean = false) extends Schema

object IntEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val required = (schema \ "required").asOpt[Boolean]

    IntEl(id, required.getOrElse(false))

  }

}


case class BooleanEl(id: Id, required: Boolean = false) extends Schema

object BooleanEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val required = (schema \ "required").asOpt[Boolean]

    BooleanEl(id, required.getOrElse(false))

  }

}


case class NullEl(id: Id, required: Boolean = false) extends Schema

object NullEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val required = (schema \ "required").asOpt[Boolean]

    NullEl(id, required.getOrElse(false))

  }

}


case class EnumEl(id: Id, choices: List[String], required: Boolean = false) extends Schema

object EnumEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val choices = (schema \ "enum").asOpt[List[String]]

    val required = (schema \ "required").asOpt[Boolean]

    EnumEl(id, choices.getOrElse(List.empty), required.getOrElse(false))

  }

}

trait Selection extends Schema

case class OneOf(id: Id,
                 selection: List[Schema],
                 discriminatorField: Option[String] = None) extends Selection

case class AnyOf(id: Id, selection: List[Schema]) extends Selection

case class AllOf(id: Id, selection: List[Schema]) extends Selection
