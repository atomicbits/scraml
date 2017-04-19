/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.scalaplay

import io.atomicbits.scraml.dsl.scalaplay.json.JsonOps
import play.api.libs.json.Format

/**
  * Created by peter on 27/07/15.
  */
sealed trait HttpParam

case class SimpleHttpParam(parameter: String) extends HttpParam

object SimpleHttpParam {

  def create(value: Any): SimpleHttpParam = SimpleHttpParam(value.toString)

}

case class ComplexHttpParam(json: String) extends HttpParam

object ComplexHttpParam {

  def create[P](value: P)(implicit formatter: Format[P]): ComplexHttpParam =
    ComplexHttpParam(JsonOps.toString(formatter.writes(value)))

}

case class RepeatedHttpParam(parameters: List[String]) extends HttpParam

object RepeatedHttpParam {

  def create(values: List[Any]): RepeatedHttpParam = RepeatedHttpParam(values.map(_.toString))

}
