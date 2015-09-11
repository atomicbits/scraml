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

  protected def requestBuilder: RequestBuilder

}


/**
 * We DON'T use case classes here to hide the internals from the resulting DSL.
 */
class PlainSegment(pathElement: String, req: RequestBuilder) extends Segment {

  protected val requestBuilder = req

}

class ParamSegment[T](value: T, req: RequestBuilder) extends Segment {

  protected val requestBuilder = req

}


abstract class MethodSegment[B, R](method: Method,
                                   theBody: Option[B],
                                   queryParams: Map[String, Option[HttpParam]],
                                   formParams: Map[String, Option[HttpParam]],
                                   multipartParams: List[BodyPart],
                                   validAcceptHeaders: List[String],
                                   validContentTypeHeaders: List[String],
                                   req: RequestBuilder) extends Segment {

  val body = theBody

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val formParameterMap = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    method = method,
    queryParameters = queryParameterMap,
    formParameters = formParameterMap,
    multipartParams = multipartParams,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class StringMethodSegment[B](method: Method,
                             theBody: Option[B] = None,
                             queryParams: Map[String, Option[HttpParam]],
                             formParams: Map[String, Option[HttpParam]] = Map.empty,
                             multipartParams: List[BodyPart] = List.empty,
                             validAcceptHeaders: List[String],
                             validContentTypeHeaders: List[String],
                             req: RequestBuilder)
  extends MethodSegment[B, String](method, theBody, queryParams, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[String]): Future[Response[String]] =
    requestBuilder.callToStringResponse[B](body)

}


class JsonMethodSegment[B](method: Method,
                           theBody: Option[B] = None,
                           queryParams: Map[String, Option[HttpParam]],
                           formParams: Map[String, Option[HttpParam]] = Map.empty,
                           multipartParams: List[BodyPart] = List.empty,
                           validAcceptHeaders: List[String],
                           validContentTypeHeaders: List[String],
                           req: RequestBuilder)
  extends MethodSegment[B, JsValue](method, theBody, queryParams, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[JsValue]): Future[Response[JsValue]] =
    requestBuilder.callToJsonResponse[B](body)

}


class TypeMethodSegment[B, R](method: Method,
                              theBody: Option[B] = None,
                              queryParams: Map[String, Option[HttpParam]] = Map.empty,
                              formParams: Map[String, Option[HttpParam]] = Map.empty,
                              multipartParams: List[BodyPart] = List.empty,
                              validAcceptHeaders: List[String] = List.empty,
                              validContentTypeHeaders: List[String] = List.empty,
                              req: RequestBuilder)
  extends MethodSegment[B, R](method, theBody, queryParams, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req) {

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] =
    requestBuilder.callToTypeResponse[B, R](body)

}
