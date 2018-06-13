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
