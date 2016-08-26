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

package io.atomicbits.scraml.ramlparser.model

import play.api.libs.json.{JsNull, JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 10/02/16.
  */
sealed trait ParameterType {

  def required: Boolean

  def asRequired: ParameterType

}


object ParameterType {

  def unapply(json: JsValue): Option[ParameterType] = {
    json match {
      case StringType(stringType)                   => Option(stringType)
      case NumberType(numberType)                   => Option(numberType)
      case IntegerType(integerType)                 => Option(integerType)
      case BooleanType(booleanType)                 => Option(booleanType)
      case DateOnlyType(dateOnlyType)               => Option(dateOnlyType)
      case TimeOnlyType(timeOnlyType)               => Option(timeOnlyType)
      case DateTimeOnlyType(dateTimeOnlyType)       => Option(dateTimeOnlyType)
      case DateTimeDefaultType(dateTimeDefaultType) => Option(dateTimeDefaultType)
      case DateTimeRFC2616Type(dateTimeRfc2616Type) => Option(dateTimeRfc2616Type)
      case FileType(fileType)                       => Option(fileType)
      case ObjectType(objectType)                   => Option(objectType)
      case ArrayType(arrayType)                     => Option(arrayType)
      case _                                        => None
    }
  }

}


case class StringType(pattern: Option[String] = None,
                      minLength: Option[Int] = None,
                      maxLength: Option[Int] = None,
                      required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object StringType {

  val value = "string"

  def unapply(json: JsValue): Option[StringType] = {
    json match {
      case JsNull => Some(StringType())
      case _      =>
        (json \ "type").toOption.collect {
          case JsString(StringType.value) =>
            StringType(
              pattern = json.fieldStringValue("pattern"),
              minLength = json.fieldIntValue("minLength"),
              maxLength = json.fieldIntValue("maxLength"),
              required = json.fieldBooleanValue("required").getOrElse(false)
            )
        }
    }
  }

}


case class NumberType(format: Option[String] = None,
                      minimum: Option[Int] = None,
                      maximum: Option[Int] = None,
                      multipleOf: Option[Int] = None,
                      required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object NumberType {

  val value = "number"

  def unapply(json: JsValue): Option[NumberType] = {
    (json \ "type").toOption.collect {
      case JsString(NumberType.value) =>
        NumberType(
          format = json.fieldStringValue("format"),
          minimum = json.fieldIntValue("minimum"),
          maximum = json.fieldIntValue("maximum"),
          multipleOf = json.fieldIntValue("multipleOf"),
          required = json.fieldBooleanValue("required").getOrElse(false)
        )
    }
  }

}


case class IntegerType(format: Option[String] = None,
                       minimum: Option[Int] = None,
                       maximum: Option[Int] = None,
                       multipleOf: Option[Int] = None,
                       required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object IntegerType {

  val value = "integer"

  def unapply(json: JsValue): Option[IntegerType] = {
    (json \ "type").toOption.collect {
      case JsString(IntegerType.value) =>
        IntegerType(
          format = json.fieldStringValue("format"),
          minimum = json.fieldIntValue("minimum"),
          maximum = json.fieldIntValue("maximum"),
          multipleOf = json.fieldIntValue("multipleOf"),
          required = json.fieldBooleanValue("required").getOrElse(false)
        )
    }
  }

}


case class BooleanType(required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object BooleanType {

  val value = "boolean"

  def unapply(json: JsValue): Option[BooleanType] = {
    (json \ "type").toOption.collect {
      case JsString(BooleanType.value) => BooleanType(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}


case class FileType(fileTypes: Option[Seq[String]] = None,
                    minLength: Option[Int] = None,
                    maxLength: Option[Int] = None,
                    required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object FileType {

  val value = "file"

  def unapply(json: JsValue): Option[FileType] = {
    (json \ "type").toOption.collect {
      case JsString(FileType.value) =>
        FileType(
          fileTypes = json.fieldStringListValue("fileTypes"),
          minLength = json.fieldIntValue("minLength"),
          maxLength = json.fieldIntValue("maxLength"),
          required = json.fieldBooleanValue("required").getOrElse(false)
        )
    }
  }

}


case class ObjectType(required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object ObjectType {

  val value = "object"

  def unapply(json: JsValue): Option[ObjectType] = {
    (json \ "type").toOption.collect {
      case JsString(ObjectType.value) => ObjectType(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}

case class ArrayType(items: ParameterType,
                     required: Boolean = true) extends ParameterType {

  def asRequired = copy(required = true)

}

case object ArrayType {

  val value = "array"

  def unapply(json: JsValue): Option[ArrayType] = {

    (json \ "type").toOption.collect {
      case JsString(ArrayType.value) =>
        val items =
          (json \ "items").toOption.collect {
            case ParameterType(itemsType) => itemsType
          } getOrElse StringType()

        ArrayType(
          items = items,
          required = json.fieldBooleanValue("required").getOrElse(false)
        )
    }
  }

}


sealed trait DateType extends ParameterType {

  def format: DateFormat

}

/**
  * date-only	The "full-date" notation of RFC3339, namely yyyy-mm-dd. Does not support time or time zone-offset notation.
  *
  * example: 2015-05-23
  */
case class DateOnlyType(required: Boolean = true) extends DateType {

  val format = RFC3339FullDate

  def asRequired = copy(required = true)

}

case object DateOnlyType {

  val value = "date-only"

  def unapply(json: JsValue): Option[DateType] = {
    (json \ "type").toOption.collect {
      case JsString(DateOnlyType.value) => DateOnlyType(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}

/**
  * time-only	The "partial-time" notation of RFC3339, namely hh:mm:ss[.ff...]. Does not support date or time zone-offset notation.
  *
  * example: 12:30:00
  */
case class TimeOnlyType(required: Boolean = true) extends DateType {

  val format = RFC3339PartialTime

  def asRequired = copy(required = true)

}

case object TimeOnlyType {

  val value = "time-only"

  def unapply(json: JsValue): Option[DateType] = {
    (json \ "type").toOption.collect {
      case JsString(TimeOnlyType.value) => TimeOnlyType(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}

/**
  * datetime-only	Combined date-only and time-only with a separator of "T", namely yyyy-mm-ddThh:mm:ss[.ff...]. Does not support
  * a time zone offset.
  *
  * example: 2015-07-04T21:00:00
  */
case class DateTimeOnlyType(required: Boolean = true) extends DateType {

  val format = DateOnlyTimeOnly

  def asRequired = copy(required = true)

}

case object DateTimeOnlyType {

  val value = "datetime-only"

  def unapply(json: JsValue): Option[DateType] = {
    (json \ "type").toOption.collect {
      case JsString(DateTimeOnlyType.value) => DateTimeOnlyType(required = json.fieldBooleanValue("required").getOrElse(false))
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


case class DateTimeDefaultType(required: Boolean = true) extends DateTimeType {

  val format = RFC3339DateTime

  def asRequired = copy(required = true)

}

case object DateTimeDefaultType {

  def unapply(json: JsValue): Option[DateTimeType] = {

    val isValid = json.fieldStringValue("format").forall(_.toLowerCase == "rfc3339")

    (json \ "type").toOption.collect {
      case JsString(DateTimeType.value) if isValid => DateTimeDefaultType(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}


case class DateTimeRFC2616Type(required: Boolean = true) extends DateTimeType {

  val format = RFC2616

  def asRequired = copy(required = true)

}

case object DateTimeRFC2616Type {

  def unapply(json: JsValue): Option[DateType] = {

    val isValid = json.fieldStringValue("format").exists(_.toLowerCase == "rfc2616")

    (json \ "type").toOption.collect {
      case JsString(DateTimeType.value) if isValid => DateTimeRFC2616Type(required = json.fieldBooleanValue("required").getOrElse(false))
    }
  }

}
