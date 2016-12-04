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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.JsValue

import scala.util.Try

/**
  * Created by peter on 10/02/16.
  */
trait Type extends Identifiable {

  def required: Option[Boolean]

  // The default value according to the RAML 1.0 specs is true. According to the json-schema 0.3 specs, it should be false.
  // The json-schema 0.4 specs don't specify a 'required: boolean' field any more, only a list of the required field names at the
  // level of the object definition, but this also implies a default value of false.
  def isRequired = required.getOrElse(defaultRequiredValue)

  def defaultRequiredValue = model match {
    case JsonSchemaModel => false
    case RamlModel       => true
  }

  def updated(id: Id): Type

  def asTypeModel(typeModel: TypeModel): Type

  def model: TypeModel

}


trait Identifiable {

  def id: Id

  def updated(id: Id): Identifiable

}


trait PrimitiveType extends Type

trait NonPrimitiveType extends Type


/**
  * Only used in json-schema.
  */
trait Fragmented {

  def fragments: Fragments

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
      case StringType.value        => Try(new StringType())
      case NumberType.value        => Try(new NumberType())
      case IntegerType.value       => Try(new IntegerType())
      case BooleanType.value       => Try(new BooleanType())
      case NullType.value          => Try(new StringType())
      case UnionType(tryUnionType) => tryUnionType
      case ArrayType(tryArrayType) => tryArrayType
      case namedType               => Try(TypeReference(NativeId(namedType)))
    }

  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Type]] = {

    val result =
      json match {
        case MultipleInheritanceType(tryMultiType)    => Some(tryMultiType)
        case ArrayType(tryArrayType)                  => Some(tryArrayType) // ArrayType must stay on the second spot of this pattern match!
        case UnionType(tryUnionType)                  => Some(tryUnionType)
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
        case ObjectType(tryObjectType)                => Some(tryObjectType)
        case GenericObjectType(tryGenericObjectType)  => Some(tryGenericObjectType)
        case TypeReference(tryTypeReferenceType)      => Some(tryTypeReferenceType)
        case InlineTypeDeclaration(inlineType)        => Some(inlineType)
        case _                                        => None
      }

    result
  }


  def typeDeclaration(json: JsValue): Option[JsValue] = {
    List((json \ "type").toOption, (json \ "schema").toOption).flatten.headOption
  }


}


object PrimitiveType {

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[PrimitiveType]] = {
    json match {
      case StringType(triedStringType)   => Some(triedStringType)
      case NumberType(triedNumberType)   => Some(triedNumberType)
      case IntegerType(triedIntegerType) => Some(triedIntegerType)
      case BooleanType(triedBooleanType) => Some(triedBooleanType)
      case NullType(triedNullType)       => Some(triedNullType)
      case _                             => None
    }
  }

}