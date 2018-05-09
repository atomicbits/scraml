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

import io.atomicbits.scraml.dsl.scalaplay.client.ClientConfig
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io).
  */
trait Client extends AutoCloseable {

  def config: ClientConfig

  def defaultHeaders: Map[String, String]

  def callToStringResponse(request: RequestBuilder, body: Option[String]): Future[Response[String]]

  def callToJsonResponse(requestBuilder: RequestBuilder, body: Option[String]): Future[Response[JsValue]]

  def callToTypeResponse[R](request: RequestBuilder, body: Option[String])(implicit responseFormat: Format[R]): Future[Response[R]]

  def callToBinaryResponse(request: RequestBuilder, body: Option[String]): Future[Response[BinaryData]]

  def close(): Unit

}
