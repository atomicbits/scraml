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

import play.api.libs.json.JsValue

/**
  * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io).
  */
case class Response[T](status: Int,
                       stringBody: Option[String],
                       jsonBody: Option[JsValue]          = None,
                       body: Option[T]                    = None,
                       headers: Map[String, List[String]] = Map.empty) {

  def map[S](f: T => S) = {
    this.copy(body = body.map(f))
  }

  def flatMap[S](f: T => Option[S]) = {
    this.copy(body = body.flatMap(f))
  }

}
