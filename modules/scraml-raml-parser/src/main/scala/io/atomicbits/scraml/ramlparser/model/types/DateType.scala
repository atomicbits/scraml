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

import play.api.libs.json.{JsString, JsValue}
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.util.{Success, Try}


/**
  * Created by peter on 26/08/16.
  */
trait DateType extends PrimitiveType with AllowedAsObjectField {

  def format: DateFormat

}

/**
  * date-only	The "full-date" notation of RFC3339, namely yyyy-mm-dd. Does not support time or time zone-offset notation.
  *
  * example: 2015-05-23
  */
case class DateOnlyType(id: Id = ImplicitId, required: Option[Boolean] = None) extends DateType {

  val format = RFC3339FullDate

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

}


case object DateOnlyType {

  val value = "date-only"

  def unapply(json: JsValue): Option[Try[DateOnlyType]] = {
    Type.typeDeclaration(json).collect {
      case JsString(DateOnlyType.value) => Success(DateOnlyType(required = json.fieldBooleanValue("required")))
    }
  }

}


/**
  * time-only	The "partial-time" notation of RFC3339, namely hh:mm:ss[.ff...]. Does not support date or time zone-offset notation.
  *
  * example: 12:30:00
  */
case class TimeOnlyType(id: Id = ImplicitId, required: Option[Boolean] = None) extends DateType {

  val format = RFC3339PartialTime

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

}


case object TimeOnlyType {

  val value = "time-only"

  def unapply(json: JsValue): Option[Try[TimeOnlyType]] = {
    Type.typeDeclaration(json).collect {
      case JsString(TimeOnlyType.value) => Success(TimeOnlyType(required = json.fieldBooleanValue("required")))
    }
  }

}


/**
  * datetime-only	Combined date-only and time-only with a separator of "T", namely yyyy-mm-ddThh:mm:ss[.ff...]. Does not support
  * a time zone offset.
  *
  * example: 2015-07-04T21:00:00
  */
case class DateTimeOnlyType(id: Id = ImplicitId, required: Option[Boolean] = None) extends DateType {

  val format = DateOnlyTimeOnly

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): DateTimeOnlyType = copy(id = updatedId)

}


case object DateTimeOnlyType {

  val value = "datetime-only"

  def unapply(json: JsValue): Option[Try[DateTimeOnlyType]] = {
    Type.typeDeclaration(json).collect {
      case JsString(DateTimeOnlyType.value) => Success(DateTimeOnlyType(required = json.fieldBooleanValue("required")))
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
sealed trait DateTimeType extends DateType

object DateTimeType {

  val value = "datetime"

}


case class DateTimeDefaultType(id: Id = ImplicitId, required: Option[Boolean] = None) extends DateTimeType {

  val format = RFC3339DateTime

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

}


case object DateTimeDefaultType {

  def unapply(json: JsValue): Option[Try[DateTimeDefaultType]] = {

    val isValid = json.fieldStringValue("format").forall(_.toLowerCase == "rfc3339")

    Type.typeDeclaration(json).collect {
      case JsString(DateTimeType.value) if isValid =>
        Success(DateTimeDefaultType(required = json.fieldBooleanValue("required")))
    }
  }

}


case class DateTimeRFC2616Type(id: Id = ImplicitId, required: Option[Boolean] = None) extends DateTimeType {

  val format = RFC2616

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

}


case object DateTimeRFC2616Type {

  def unapply(json: JsValue): Option[Try[DateTimeRFC2616Type]] = {

    val isValid = json.fieldStringValue("format").exists(_.toLowerCase == "rfc2616")

    Type.typeDeclaration(json).collect {
      case JsString(DateTimeType.value) if isValid =>
        Success(DateTimeRFC2616Type(required = json.fieldBooleanValue("required")))
    }
  }

}
