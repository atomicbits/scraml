/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

}

trait Identifiable {

  def id: Id

  def updated(id: Id): Identifiable

  def asTypeModel(typeModel: TypeModel): ParsedType

  def model: TypeModel

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
            case Some(frag: Fragmented) => (Some(frag), next)
            case _                        => (None, next)
          }
        case ((None, _), _) => (None, None)
      }

    identifiableOpt
  }

}

trait AllowedAsObjectField {}

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
        case ParsedMultipleInheritance(tryMultiType)    => Some(tryMultiType)
        case ParsedArray(tryArrayType)                  => Some(tryArrayType) // ArrayType must stay on the second spot of this pattern match!
        case ParsedUnionType(tryUnionType)              => Some(tryUnionType)
        case ParsedString(tryStringType)                => Some(tryStringType)
        case ParsedNumber(tryNumberType)                => Some(tryNumberType)
        case ParsedInteger(tryIntegerType)              => Some(tryIntegerType)
        case ParsedBoolean(tryBooleanType)              => Some(tryBooleanType)
        case ParsedDateOnly(dateOnlyType)               => Some(dateOnlyType)
        case ParsedTimeOnly(timeOnlyType)               => Some(timeOnlyType)
        case ParsedDateTimeOnly(dateTimeOnlyType)       => Some(dateTimeOnlyType)
        case ParsedDateTimeDefault(dateTimeDefaultType) => Some(dateTimeDefaultType)
        case ParsedDateTimeRFC2616(dateTimeRfc2616Type) => Some(dateTimeRfc2616Type)
        case ParsedFile(fileType)                       => Some(fileType)
        case ParsedNull(tryNullType)                    => Some(tryNullType)
        case ParsedEnum(tryEnumType)                    => Some(tryEnumType)
        case ParsedObject(tryObjectType)                => Some(tryObjectType)
        case ParsedGenericObject(tryGenericObjectType)  => Some(tryGenericObjectType)
        case ParsedTypeReference(tryTypeReferenceType)  => Some(tryTypeReferenceType)
        case InlineTypeDeclaration(inlineType)          => Some(inlineType)
        case ParsedFragmentContainer(tryFragmentCont)   => Some(tryFragmentCont)
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
