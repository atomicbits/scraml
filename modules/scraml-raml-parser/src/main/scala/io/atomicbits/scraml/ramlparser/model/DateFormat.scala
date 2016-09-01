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

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
  * Created by peter on 19/08/16.
  */
sealed trait DateFormat {

  def pattern: String

  def formatter: DateTimeFormatter

  def parse(dateTime: String): OffsetDateTime = OffsetDateTime.parse(dateTime, formatter)

}

case object RFC3339FullDate extends DateFormat {

  val pattern = "yyyy-MM-dd"

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)

}

case object RFC3339PartialTime extends DateFormat {

  val pattern = "HH:mm:ss[.SSS]"

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)

}

case object DateOnlyTimeOnly extends DateFormat {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)

  // e.g. 2015-07-04T21:00:00

  // "yyyy-MM-dd[[ ]['T']HH:mm[:ss][XXX]]"
  // "yyyy-MM-dd'T'HH:mm:ss[.SSS]"

}

case object RFC3339DateTime extends DateFormat {

  val pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]XXX"

  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  // e.g. 2016-02-28T16:41:41.090Z
  // e.g. 2016-02-28T16:41:41.090+08:00

}

case object RFC2616 extends DateFormat {

  val pattern = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"

  // e.g. Sun, 28 Feb 2016 16:41:41 GMT

  val formatter: DateTimeFormatter  = DateTimeFormatter.RFC_1123_DATE_TIME


  /**
    * see: http://stackoverflow.com/questions/7707555/getting-date-in-http-format-in-java
    *
    * java8 time:
    * java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")))
    *
    * joda time:
    * private static final DateTimeFormatter RFC1123_DATE_TIME_FORMATTER = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC().withLocale(Locale.US);
    */

}

