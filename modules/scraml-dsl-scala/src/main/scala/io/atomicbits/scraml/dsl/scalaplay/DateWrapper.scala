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

package io.atomicbits.scraml.dsl.scalaplay

import java.time.{ LocalDate, LocalDateTime, LocalTime, OffsetDateTime }
import java.time.format.DateTimeFormatter

import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 7/10/17.
  */
trait DateWrapper

case class DateTimeRFC3339(dateTime: OffsetDateTime) extends DateWrapper

object DateTimeRFC3339 {

  // DateTimeFormatter.ISO_OFFSET_DATE_TIME
  // "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX"

  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def parse(dateTime: String): DateTimeRFC3339 = DateTimeRFC3339(OffsetDateTime.parse(dateTime, formatter))

  def format(dateTimeRFC3339: DateTimeRFC3339): String = formatter.format(dateTimeRFC3339.dateTime)

  implicit val jsonFormat = new Format[DateTimeRFC3339] {

    override def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(parse(s)) match {
          case Success(dateTime) => JsSuccess(dateTime)
          case Failure(_)        => JsError("error.expected.RFC3339DateTime")
        }
      case _ => JsError("error.expected.jsstring")
    }

    override def writes(dateTime: DateTimeRFC3339): JsValue = JsString(format(dateTime))

  }

}

/**
  * RFC2616: https://www.ietf.org/rfc/rfc2616.txt
  * HTTP date: rfc1123-date (| rfc850-date | asctime-date)
  */
case class DateTimeRFC2616(dateTime: OffsetDateTime) extends DateWrapper

object DateTimeRFC2616 {

  // DateTimeFormatter.RFC_1123_DATE_TIME
  // "EEE, dd MMM yyyy HH:mm:ss 'GMT'"

  val formatter: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

  def parse(dateTime: String): DateTimeRFC2616 = DateTimeRFC2616(OffsetDateTime.parse(dateTime, formatter))

  def format(dateTimeRFC2616: DateTimeRFC2616): String = formatter.format(dateTimeRFC2616.dateTime)

  implicit val jsonFormat = new Format[DateTimeRFC2616] {

    override def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(parse(s)) match {
          case Success(dateTime) => JsSuccess(dateTime)
          case Failure(_)        => JsError("error.expected.RFC1123DateTime")
        }
      case _ => JsError("error.expected.jsstring")
    }

    override def writes(dateTime: DateTimeRFC2616): JsValue = JsString(format(dateTime))

  }

}

case class DateTimeOnly(dateTime: LocalDateTime) extends DateWrapper

object DateTimeOnly {

  // DateTimeFormatter.ISO_LOCAL_DATE_TIME
  // "yyyy-MM-dd'T'HH:mm:ss[.SSS]"

  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  def parse(dateTime: String): DateTimeOnly = DateTimeOnly(LocalDateTime.parse(dateTime, formatter))

  def format(dateTimeOnly: DateTimeOnly): String = formatter.format(dateTimeOnly.dateTime)

  implicit val jsonFormat = new Format[DateTimeOnly] {

    override def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(parse(s)) match {
          case Success(dateTimeOnly) => JsSuccess(dateTimeOnly)
          case Failure(_)            => JsError("error.expected.DateTimeOnly")
        }
      case _ => JsError("error.expected.jsstring")
    }

    override def writes(dateTimeOnly: DateTimeOnly): JsValue = JsString(format(dateTimeOnly))

  }

}

case class TimeOnly(time: LocalTime) extends DateWrapper

object TimeOnly {

  // DateTimeFormatter.ISO_LOCAL_TIME
  // "HH:mm:ss[.SSS]"

  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

  def parse(time: String): TimeOnly = TimeOnly(LocalTime.parse(time, formatter))

  def format(timeOnly: TimeOnly): String = formatter.format(timeOnly.time)

  implicit val jsonFormat = new Format[TimeOnly] {

    override def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(parse(s)) match {
          case Success(timeOnly) => JsSuccess(timeOnly)
          case Failure(_)        => JsError("error.expected.TimeOnly")
        }
      case _ => JsError("error.expected.jsstring")
    }

    override def writes(timeOnly: TimeOnly): JsValue = JsString(format(timeOnly))

  }

}

case class DateOnly(date: LocalDate) extends DateWrapper

object DateOnly {

  // DateTimeFormatter.ISO_LOCAL_DATE
  // "yyyy-MM-dd"

  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  def parse(date: String): DateOnly = DateOnly(LocalDate.parse(date, formatter))

  def format(dateOnly: DateOnly): String = formatter.format(dateOnly.date)

  implicit val jsonFormat = new Format[DateOnly] {

    override def reads(json: JsValue) = json match {
      case JsString(s) =>
        Try(parse(s)) match {
          case Success(dateOnly) => JsSuccess(dateOnly)
          case Failure(_)        => JsError("error.expected.DateOnly")
        }
      case _ => JsError("error.expected.jsstring")
    }

    override def writes(dateOnly: DateOnly): JsValue = JsString(format(dateOnly))

  }

}
