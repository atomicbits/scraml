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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

import io.atomicbits.scraml.ramlparser.model._

/**
  * Created by peter on 11/12/16.
  */
trait DateType extends PrimitiveType {

  def format: DateFormat

}

case object DateOnlyType extends DateType {

  val format = RFC3339FullDate

  val canonicalName = CanonicalName.create("DateOnly")

  val refers: CanonicalName = canonicalName

}

case object TimeOnlyType extends DateType {

  val format = RFC3339PartialTime

  val canonicalName = CanonicalName.create("TimeOnly")

  val refers: CanonicalName = canonicalName

}

case object DateTimeOnlyType extends DateType {

  val format = DateOnlyTimeOnly

  val canonicalName = CanonicalName.create("DateTimeOnly")

  val refers: CanonicalName = canonicalName

}

case object DateTimeDefaultType extends DateType {

  val format = RFC3339DateTime

  val canonicalName = CanonicalName.create("DateTimeRFC3339")

  val refers: CanonicalName = canonicalName

}

case object DateTimeRFC2616Type extends DateType {

  val format = RFC2616

  val canonicalName = CanonicalName.create("DateTimeRFC2616")

  val refers: CanonicalName = canonicalName

}
