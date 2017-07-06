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

import play.api.libs.json._

import scala.language.postfixOps

/**
  * Created by peter on 14/05/17.
  */
case class TypedQueryParams(params: Map[String, HttpParam] = Map.empty) {

  def isEmpty: Boolean = params.isEmpty

}

object TypedQueryParams {

  def create[T](value: T)(implicit formatter: Format[T]): TypedQueryParams = {
    val json: JsValue = formatter.writes(value)
    TypedQueryParams(HttpParam.toFormUrlEncoded(json))
  }

}