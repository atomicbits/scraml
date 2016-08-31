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

import io.atomicbits.scraml.ramlparser.model.{Id, ImplicitId}
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 10/02/16.
  */
trait Type extends Identifiable {

  def required: Option[Boolean]

  // The default value according to the RAML 1.0 specs is true. According to the json-schema 0.3 specs, it should be false.
  // The json-schema 0.4 specs don't specify a 'required: boolean' field any more, only a list of the required field names at the
  // level of the object definition, but this also implies a default value of false.
  def isRequired = required.getOrElse(true)

}

trait Identifiable {

  def id: Id

  def updated(id: Id): Identifiable

}


trait PrimitiveType extends Type

trait NonePrimitiveType extends Type


/**
  * Only used in json-schema.
  */
trait Fragmented {

  def fragments: Fragment

  def fragment(field: String): Option[Identifiable] = fragments.fragmentMap.get(field)

  def fragment(fields: List[String]): Option[Identifiable] = {

    val aggregate: (Option[Fragmented], Option[Identifiable]) = (Some(this), None)

    val (_, identifiableOpt) =
      fields.foldLeft(aggregate) {
        case ((Some(fragm), _), field) =>
          val next = fragm.fragment(field)
          next match {
            case frag: Option[Fragmented] => (frag, next)
            case _                        => (None, next)
          }
        case ((None, _), _)            => (None, None)
      }

    identifiableOpt
  }

}


trait AllowedAsObjectField {


}


object Type {


  def apply(typeName: String)(implicit parseContext: ParseContext): Try[Type] = {

    typeName match {
      case StringType.value     => Try(new StringType())
      case NumberType.value     => Try(new NumberType())
      case IntegerType.value    => Try(new IntegerType())
      case BooleanType.value    => Try(new BooleanType())
      case NullType.value       => Try(new StringType())
      case ArrayType(arrayType) => arrayType
      case namedType            => Try(TypeReference(parseContext.nameToId(namedType)))
    }

  }


  //  def apply(schema: JsObject, nameOpt: Option[String] = None)(implicit parseContext: ParseContext): Try[Type] = {
  //
  //    val typeOpt = (schema \ "type").asOpt[String]
  //    val enumOpt = (schema \ "enum").asOpt[List[String]]
  //
  //    typeOpt match {
  //      case Some("object")  =>
  //        (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema, nameOpt)
  //      case Some("array")   => ArrayType(schema)
  //      case Some("string")  =>
  //        enumOpt match {
  //          case Some(enum) => EnumType(schema)
  //          case None       => StringType(schema)
  //        }
  //      case Some("number")  => NumberType(schema)
  //      case Some("integer") => IntegerType(schema)
  //      case Some("boolean") => BooleanType(schema)
  //      case Some("null")    => NullType(schema)
  //      case Some(namedType) =>
  //        sys.error(s"Unkown json-schema type $namedType") // In RAML 1.0 this can be 'User' or 'Phone | Notebook' or 'Email[]'
  //      case None            =>
  //        val propertiesOpt = (schema \ "properties").asOpt[String]
  //        val referenceOpt = (schema \ "$ref").asOpt[String]
  //        val enumOpt = (schema \ "enum").asOpt[List[String]]
  //        (propertiesOpt, referenceOpt, enumOpt) match {
  //          case (Some(properties), _, _)   =>
  //            (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema, None)
  //          case (None, Some(reference), _) => TypeReference(schema)
  //          case (None, None, Some(enum))   => EnumType(schema)
  //          case _                          =>
  //            // According to the RAML 1.0 defaults, if no 'type' field and no 'properties' field is present, the type defaults to a
  //            // string type. This, however, conflicts with the possibility of having nested json-schema schemas. We decided to only
  //            // interpret the type as a string if the fragment alternative (meaning we have nested schemas) is empty.
  //            Fragment(schema).flatMap { fragment =>
  //              if (fragment.isEmpty) StringType(schema)
  //              else Success(fragment)
  //            }
  //        }
  //    }
  //  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Type]] = {

    val result =
      json match {
        case StringType(tryStringType)                => Some(tryStringType)
        case NumberType(tryNumberType)                => Some(tryNumberType)
        case IntegerType(tryIntegerType)              => Some(tryIntegerType)
        case BooleanType(tryBooleanType)              => Some(tryBooleanType)
        case DateOnlyType(dateOnlyType)               => Some(dateOnlyType)
        case TimeOnlyType(timeOnlyType)               => Some(timeOnlyType)
        case DateTimeOnlyType(dateTimeOnlyType)       => Some(dateTimeOnlyType)
        case DateTimeDefaultType(dateTimeDefaultType) => Some(dateTimeDefaultType)
        case DateTimeRFC2616Type(dateTimeRfc2616Type) => Some(dateTimeRfc2616Type)
        case FileType(fileType)                       => Some(fileType)
        case NullType(tryNullType)                    => Some(tryNullType)
        case EnumType(tryEnumType)                    => Some(tryEnumType)
        case ArrayType(tryArrayType)                  => Some(tryArrayType)
        case ObjectType(tryObjectType)                => Some(tryObjectType)
        case GenericObjectType(tryGenericObjectType)  => Some(tryGenericObjectType)
        case TypeReference(tryTypeReferenceType)      => Some(tryTypeReferenceType)
        case _                                        => None
      }

    result
  }


  def typeDeclaration(json: JsValue): Option[JsValue] = {
    List((json \ "type").toOption, (json \ "schema").toOption).flatten.headOption
  }


}
