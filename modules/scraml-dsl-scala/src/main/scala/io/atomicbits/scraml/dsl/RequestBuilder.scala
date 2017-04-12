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

package io.atomicbits.scraml.dsl

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

  def relativePath = reversePath.reverse.mkString("/", "/", "")

  def defaultHeaders = client.defaultHeaders

  def allHeaders = HeaderMap() ++ (defaultHeaders.toList: _*) ++ headers // headers last to overwrite defaults!

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
