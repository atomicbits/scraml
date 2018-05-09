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

import play.api.libs.json.{ JsString, JsValue }
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.util.{ Success, Try }

/**
  * Created by peter on 26/08/16.
  */
trait ParsedDate extends NonPrimitiveType with AllowedAsObjectField {

  def format: DateFormat

  def model: TypeModel = RamlModel

  def asTypeModel(typeModel: TypeModel): ParsedType = this

}

/**
  * date-only	The "full-date" notation of RFC3339, namely yyyy-mm-dd. Does not support time or time zone-offset notation.
  *
  * example: 2015-05-23
  */
case class ParsedDateOnly(id: Id = ImplicitId, required: Option[Boolean] = None) extends ParsedDate {

  val format = RFC3339FullDate

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedDateOnly = copy(id = updatedId)

}

case object ParsedDateOnly {

  val value = "date-only"

  def unapply(json: JsValue): Option[Try[ParsedDateOnly]] = {
    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedDateOnly.value) => Success(ParsedDateOnly(required = json.fieldBooleanValue("required")))
    }
  }

}

/**
  * time-only	The "partial-time" notation of RFC3339, namely hh:mm:ss[.ff...]. Does not support date or time zone-offset notation.
  *
  * example: 12:30:00
  */
case class ParsedTimeOnly(id: Id = ImplicitId, required: Option[Boolean] = None) extends ParsedDate {

  val format = RFC3339PartialTime

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedTimeOnly = copy(id = updatedId)

}

case object ParsedTimeOnly {

  val value = "time-only"

  def unapply(json: JsValue): Option[Try[ParsedTimeOnly]] = {
    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedTimeOnly.value) => Success(ParsedTimeOnly(required = json.fieldBooleanValue("required")))
    }
  }

}

/**
  * datetime-only	Combined date-only and time-only with a separator of "T", namely yyyy-mm-ddThh:mm:ss[.ff...]. Does not support
  * a time zone offset.
  *
  * example: 2015-07-04T21:00:00
  */
case class ParsedDateTimeOnly(id: Id = ImplicitId, required: Option[Boolean] = None) extends ParsedDate {

  val format = DateOnlyTimeOnly

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedDateTimeOnly = copy(id = updatedId)

}

case object ParsedDateTimeOnly {

  val value = "datetime-only"

  def unapply(json: JsValue): Option[Try[ParsedDateTimeOnly]] = {
    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedDateTimeOnly.value) => Success(ParsedDateTimeOnly(required = json.fieldBooleanValue("required")))
    }
  }

}

/**
  * datetime	A timestamp in one of the following formats: if the format is omitted or set to rfc3339, uses the "date-time" notation
  * of RFC3339; if format is set to rfc2616, uses the format defined in RFC2616.
  *
  * example: 2016-02-28T16:41:41.090Z
  * format: rfc3339 # the default, so no need to specify
  *
  * example: Sun, 28 Feb 2016 16:41:41 GMT
  * format: rfc2616 # this time it's required, otherwise, the example format is invalid
  */
sealed trait ParsedDateTime extends ParsedDate

object ParsedDateTime {

  val value = "datetime"

}

case class ParsedDateTimeDefault(id: Id = ImplicitId, required: Option[Boolean] = None) extends ParsedDateTime {

  val format = RFC3339DateTime

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedDateTimeDefault = copy(id = updatedId)

}

case object ParsedDateTimeDefault {

  def unapply(json: JsValue): Option[Try[ParsedDateTimeDefault]] = {

    val isValid = json.fieldStringValue("format").forall(_.toLowerCase == "rfc3339")

    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedDateTime.value) if isValid =>
        Success(ParsedDateTimeDefault(required = json.fieldBooleanValue("required")))
    }
  }

}

case class ParsedDateTimeRFC2616(id: Id = ImplicitId, required: Option[Boolean] = None) extends ParsedDateTime {

  val format = RFC2616

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedDateTimeRFC2616 = copy(id = updatedId)

}

case object ParsedDateTimeRFC2616 {

  def unapply(json: JsValue): Option[Try[ParsedDateTimeRFC2616]] = {

    val isValid = json.fieldStringValue("format").exists(_.toLowerCase == "rfc2616")

    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedDateTime.value) if isValid =>
        Success(ParsedDateTimeRFC2616(required = json.fieldBooleanValue("required")))
    }
  }

}
