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

  val canonicalName = CanonicalName("DateOnly")

  val refers: CanonicalName = canonicalName

}


case object TimeOnlyType extends DateType {

  val format = RFC3339PartialTime

  val canonicalName = CanonicalName("TimeOnly")

  val refers: CanonicalName = canonicalName

}


case object DateTimeOnlyType extends DateType {

  val format = DateOnlyTimeOnly

  val canonicalName = CanonicalName("DateTimeOnly")

  val refers: CanonicalName = canonicalName

}


case object DateTimeDefaultType extends DateType {

  val format = RFC3339DateTime

  val canonicalName = CanonicalName("DateTimeDefault")

  val refers: CanonicalName = canonicalName

}


case object DateTimeRFC2616Type extends DateType {

  val format = RFC2616

  val canonicalName = CanonicalName("DateTimeRFC2616")

  val refers: CanonicalName = canonicalName

}
