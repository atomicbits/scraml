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

import play.api.libs.json.{JsValue, Format}

import scala.concurrent.Future
import scala.language.reflectiveCalls


sealed trait Segment {

  protected def _requestBuilder: RequestBuilder

}


/**
  * We DON'T use case classes here to hide the internals from the resulting DSL.
  */
class PlainSegment(pathElement: String, _req: RequestBuilder) extends Segment {

  protected val _requestBuilder = _req

}

class ParamSegment[T](value: T, _req: RequestBuilder) extends Segment {

  protected val _requestBuilder = _req

}

class HeaderSegment(_req: RequestBuilder) extends Segment {

  protected val _requestBuilder = _req

}


abstract class MethodSegment[B, R](method: Method,
                                   theBody: Option[B],
                                   queryParams: Map[String, Option[HttpParam]],
                                   formParams: Map[String, Option[HttpParam]],
                                   multipartParams: List[BodyPart],
                                   binaryBody: Option[BinaryRequest] = None,
                                   expectedAcceptHeader: Option[String],
                                   expectedContentTypeHeader: Option[String],
                                   req: RequestBuilder) extends Segment {

  val body = theBody

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val formParameterMap = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val _requestBuilder = {
    val reqUpdated =
      req.copy(
        method = method,
        queryParameters = queryParameterMap,
        formParameters = formParameterMap,
        multipartParams = multipartParams,
        binaryBody = binaryBody
      )

    val reqWithAccept =
      expectedAcceptHeader map { acceptHeader =>
        reqUpdated.copy(headers = reqUpdated.headers + ("Accept" -> acceptHeader))
      } getOrElse reqUpdated

    expectedContentTypeHeader map { contentHeader =>
      reqWithAccept.copy(headers = reqWithAccept.headers + ("Content-Type" -> contentHeader))
    } getOrElse reqWithAccept
  }

}


class StringMethodSegment[B](method: Method,
                             theBody: Option[B] = None,
                             queryParams: Map[String, Option[HttpParam]],
                             formParams: Map[String, Option[HttpParam]] = Map.empty,
                             multipartParams: List[BodyPart] = List.empty,
                             binaryParam: Option[BinaryRequest] = None,
                             expectedAcceptHeader: Option[String] = None,
                             expectedContentTypeHeader: Option[String] = None,
                             req: RequestBuilder)
  extends MethodSegment[B, String](method, theBody, queryParams, formParams, multipartParams, binaryParam, expectedAcceptHeader, expectedContentTypeHeader, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[String]): Future[Response[String]] =
    _requestBuilder.callToStringResponse[B](body)

}


class JsonMethodSegment[B](method: Method,
                           theBody: Option[B] = None,
                           queryParams: Map[String, Option[HttpParam]],
                           formParams: Map[String, Option[HttpParam]] = Map.empty,
                           multipartParams: List[BodyPart] = List.empty,
                           binaryParam: Option[BinaryRequest] = None,
                           expectedAcceptHeader: Option[String] = None,
                           expectedContentTypeHeader: Option[String] = None,
                           req: RequestBuilder)
  extends MethodSegment[B, JsValue](method, theBody, queryParams, formParams, multipartParams, binaryParam, expectedAcceptHeader, expectedContentTypeHeader, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[JsValue]): Future[Response[JsValue]] =
    _requestBuilder.callToJsonResponse[B](body)

}


class TypeMethodSegment[B, R](method: Method,
                              theBody: Option[B] = None,
                              queryParams: Map[String, Option[HttpParam]] = Map.empty,
                              formParams: Map[String, Option[HttpParam]] = Map.empty,
                              multipartParams: List[BodyPart] = List.empty,
                              binaryParam: Option[BinaryRequest] = None,
                              expectedAcceptHeader: Option[String] = None,
                              expectedContentTypeHeader: Option[String] = None,
                              req: RequestBuilder)
  extends MethodSegment[B, R](method, theBody, queryParams, formParams, multipartParams, binaryParam, expectedAcceptHeader, expectedContentTypeHeader, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] =
    _requestBuilder.callToTypeResponse[B, R](body)

}


class BinaryMethodSegment[B](method: Method,
                             theBody: Option[B] = None,
                             queryParams: Map[String, Option[HttpParam]],
                             formParams: Map[String, Option[HttpParam]] = Map.empty,
                             multipartParams: List[BodyPart] = List.empty,
                             binaryParam: Option[BinaryRequest] = None,
                             expectedAcceptHeader: Option[String] = None,
                             expectedContentTypeHeader: Option[String] = None,
                             req: RequestBuilder)
  extends MethodSegment[B, BinaryData](method, theBody, queryParams, formParams, multipartParams, binaryParam, expectedAcceptHeader, expectedContentTypeHeader, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[String]): Future[Response[BinaryData]] =
    _requestBuilder.callToBinaryResponse[B](body)

}
