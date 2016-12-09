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
trait ParsedType extends Identifiable {

  def required: Option[Boolean]

  // The default value according to the RAML 1.0 specs is true. According to the json-schema 0.3 specs, it should be false.
  // The json-schema 0.4 specs don't specify a 'required: boolean' field any more, only a list of the required field names at the
  // level of the object definition, but this also implies a default value of false.
  def isRequired = required.getOrElse(defaultRequiredValue)

  def defaultRequiredValue = model match {
    case JsonSchemaModel => false
    case RamlModel       => true
  }

  def updated(id: Id): ParsedType

  def asTypeModel(typeModel: TypeModel): ParsedType

  def model: TypeModel

}


trait Identifiable {

  def id: Id

  def updated(id: Id): Identifiable

}


trait PrimitiveType extends ParsedType

trait NonPrimitiveType extends ParsedType


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


object ParsedType {

  def apply(typeName: String)(implicit parseContext: ParseContext): Try[ParsedType] = {

    typeName match {
      case ParsedString.value            => Try(new ParsedString())
      case ParsedNumber.value            => Try(new ParsedNumber())
      case ParsedInteger.value           => Try(new ParsedInteger())
      case ParsedBoolean.value           => Try(new ParsedBoolean())
      case ParsedNull.value              => Try(new ParsedString())
      case ParsedUnionType(tryUnionType) => tryUnionType
      case ParsedArray(tryArrayType)     => tryArrayType
      case namedType                     => Try(ParsedTypeReference(NativeId(namedType)))
    }

  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedType]] = {

    val result =
      json match {
        case ParsedMultipleInheritance(tryMultiType) => Some(tryMultiType)
        case ParsedArray(tryArrayType)               => Some(tryArrayType) // ArrayType must stay on the second spot of this pattern match!
        case ParsedUnionType(tryUnionType)           => Some(tryUnionType)
        case ParsedString(tryStringType)             => Some(tryStringType)
        case ParsedNumber(tryNumberType)             => Some(tryNumberType)
        case ParsedInteger(tryIntegerType)           => Some(tryIntegerType)
        case ParsedBoolean(tryBooleanType)           => Some(tryBooleanType)
        case ParsedDateOnly(dateOnlyType)            => Some(dateOnlyType)
        case ParsedTimeOnly(timeOnlyType)            => Some(timeOnlyType)
        case ParsedDateTimeOnly(dateTimeOnlyType)    => Some(dateTimeOnlyType)
        case ParsedDateTimeDefault(dateTimeDefaultType) => Some(dateTimeDefaultType)
        case ParsedDateTimeRFC2616(dateTimeRfc2616Type) => Some(dateTimeRfc2616Type)
        case ParsedFile(fileType)                       => Some(fileType)
        case ParsedNull(tryNullType)                    => Some(tryNullType)
        case ParsedEnum(tryEnumType)                    => Some(tryEnumType)
        case ParsedObject(tryObjectType)                => Some(tryObjectType)
        case ParsedGenericObject(tryGenericObjectType)  => Some(tryGenericObjectType)
        case ParsedTypeReference(tryTypeReferenceType)  => Some(tryTypeReferenceType)
        case InlineTypeDeclaration(inlineType)          => Some(inlineType)
        case _                                          => None
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
      case ParsedString(triedStringType)   => Some(triedStringType)
      case ParsedNumber(triedNumberType)   => Some(triedNumberType)
      case ParsedInteger(triedIntegerType) => Some(triedIntegerType)
      case ParsedBoolean(triedBooleanType) => Some(triedBooleanType)
      case ParsedNull(triedNullType)       => Some(triedNullType)
      case _                               => None
    }
  }

}