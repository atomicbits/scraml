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

import play.api.libs.json.{ Format, JsValue }

import scala.concurrent.Future

/**
  * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io).
  */
case class RequestBuilder(client: Client,
                          reversePath: List[String]               = Nil,
                          method: Method                          = Get,
                          queryParameters: Map[String, HttpParam] = Map.empty,
                          formParameters: Map[String, HttpParam]  = Map.empty,
                          multipartParams: List[BodyPart]         = List.empty,
                          binaryBody: Option[BinaryRequest]       = None,
                          headers: HeaderMap                      = HeaderMap()) {

  def relativePath: String = reversePath.reverse.mkString("/", "/", "")

  def defaultHeaders: Map[String, String] = client.defaultHeaders

  lazy val allHeaders: HeaderMap = HeaderMap() ++ (defaultHeaders.toList: _*) ++ headers // headers last to overwrite defaults!

  def isFormPost: Boolean = method == Post && formParameters.nonEmpty

  def isMultipartFormUpload: Boolean = allHeaders.get("Content-Type").exists(_.contains("multipart/form-data"))

  def callToStringResponse(body: Option[String]): Future[Response[String]] = client.callToStringResponse(this, body)

  def callToJsonResponse(body: Option[String]): Future[Response[JsValue]] = client.callToJsonResponse(this, body)

  def callToTypeResponse[R](body: Option[String])(implicit responseFormat: Format[R]): Future[Response[R]] =
    client.callToTypeResponse(this, body)

  def callToBinaryResponse(body: Option[String]): Future[Response[BinaryData]] = client.callToBinaryResponse(this, body)

  def summary: String = s"$method request to ${reversePath.reverse.mkString("/")}"

  def withAddedHeaders(additionalHeaders: (String, String)*): RequestBuilder = {
    this.copy(headers = this.headers ++ (additionalHeaders: _*))
  }

  def withSetHeaders(additionalHeaders: (String, String)*): RequestBuilder = {
    this.copy(headers = this.headers set (additionalHeaders: _*))
  }

  def withAddedPathSegment(additionalPathSegment: Any): RequestBuilder = {
    this.copy(reversePath = additionalPathSegment.toString :: this.reversePath)
  }

}
