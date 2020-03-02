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

package io.atomicbits.scraml.client.manual

import io.atomicbits.scraml.dsl.scalaplay._
import play.api.libs.json.JsValue

/**
  * Created by peter on 17/08/15.
  */
class PathparamResource(_value: String, private val _req: RequestBuilder) extends ParamSegment[String](_value, _req) {

  def withHeaders(newHeaders: (String, String)*) =
    new PathparamResource(_value, _requestBuilder.withAddedHeaders(newHeaders: _*))

  def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new TypeMethodSegment[String, User](
    method = Get,
    queryParams = Map(
      "queryparX" -> Option(queryparX).map(SimpleHttpParam.create(_)),
      "queryparY" -> Option(queryparY).map(SimpleHttpParam.create(_)),
      "queryParZ" -> queryParZ.map(SimpleHttpParam.create(_))
    ),
    queryString          = None,
    expectedAcceptHeader = Some("application/json"),
    req                  = _requestBuilder
  )

  def put(body: String) =
    new TypeMethodSegment[String, Address](
      method                    = Put,
      theBody                   = Some(body),
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

  def put(body: JsValue) =
    new TypeMethodSegment[JsValue, Address](
      method                    = Put,
      theBody                   = Some(body),
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

  def put(body: User) =
    new TypeMethodSegment[User, Address](method                    = Put,
                                         theBody                   = Some(body),
                                         expectedAcceptHeader      = Some("application/json"),
                                         expectedContentTypeHeader = Some("application/json"),
                                         req                       = _requestBuilder)

  def post(formparX: Int, formParY: Double, formParZ: Option[String]) =
    new TypeMethodSegment(
      method  = Post,
      theBody = None,
      formParams = Map(
        "formparX" -> Option(formparX).map(SimpleHttpParam.create(_)),
        "formParY" -> Option(formParY).map(SimpleHttpParam.create(_)),
        "formParZ" -> formParZ.map(SimpleHttpParam.create(_))
      ),
      multipartParams           = List.empty,
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

}
