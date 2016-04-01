/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model.types

import io.atomicbits.scraml.ramlparser.model.Id
import play.api.libs.json.{JsValue, JsObject}

import scala.util.Try

/**
  * Created by peter on 10/02/16.
  */
trait Type {

  def id: Id

  def updated(id: Id): Type

}


trait PrimitiveType extends Type


/**
  * Only used in json-schema.
  */
trait FragmentedType extends Type {

  def fragments: Map[String, Type]

}


trait AllowedAsObjectField {

  def required: Boolean

}


object Type {

  def apply(schema: JsObject, nameOpt: Option[String] = None)(implicit nameToId: String => Id): Try[Type] = {

    val typeOpt = (schema \ "type").asOpt[String]
    val enumOpt = (schema \ "enum").asOpt[List[String]]

    typeOpt match {
      case Some("object")  =>
        (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema)
      case Some("array")   => ArrayType(schema)
      case Some("string")  =>
        enumOpt match {
          case Some(enum) => EnumType(schema)
          case None       => StringType(schema)
        }
      case Some("number")  => NumberType(schema)
      case Some("integer") => IntegerType(schema)
      case Some("boolean") => BooleanType(schema)
      case Some("null")    => NullType(schema)
      case Some(namedType) =>
        sys.error(s"Unkown json-schema type $namedType") // In RAML 1.0 this can be 'User' or 'Phone | Notebook' or 'Email[]'
      case None            =>
        val propertiesOpt = (schema \ "properties").asOpt[String]
        val referenceOpt = (schema \ "$ref").asOpt[String]
        val enumOpt = (schema \ "enum").asOpt[List[String]]
        (propertiesOpt, referenceOpt, enumOpt) match {
          case (Some(properties), _, _)   =>
            (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema)
          case (None, Some(reference), _) => TypeReference(schema)
          case (None, None, Some(enum))   => EnumType(schema)
          case _                          =>
            // According to the RAML 1.0 defaults if no 'type' field and no 'properties' field is present, it defaults to a string type.
            // This, however, conflicts with nested json-schema schemas.
            Fragment(schema)
        }
    }
  }


  def collectFragments(schemaObject: JsObject): Map[String, Try[Type]] = {
    // Process the fragments and exclude the json-schema fields that we don't need to consider
    // (should be only objects as other fields are ignored as fragmens) ToDo: check this
    val keysToExclude =
      Seq("id", "type", "properties", "required", "oneOf", "anyOf", "allOf", "typeVariables", "genericTypes", "genericType")
    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](schemaObject.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }
    fragmentsToKeep collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Type(fragment))
    }
  }

}
