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


sealed trait MethodSegment[B, R] extends Segment {

  protected def body: Option[B]

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]]

  protected def callToStringResponse()(implicit bodyFormat: Format[B]) =
    requestBuilder.callToStringResponse[B](body)

  protected def callToJsonResponse()(implicit bodyFormat: Format[B]) =
    requestBuilder.callToJsonResponse[B](body)

  protected def callToTypeResponse()(implicit bodyFormat: Format[B], responseFormat: Format[R]) =
    requestBuilder.callToTypeResponse[B, R](body)

}


trait StringResponseSegment[B] {
  self: MethodSegment[B, String] =>

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[String]): Future[Response[String]] = callToStringResponse()

}

trait JsonResponseSegment[B] {
  self: MethodSegment[B, JsValue] =>

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[JsValue]): Future[Response[JsValue]] = callToJsonResponse()

}

trait TypeResponseSegment[B, R] {
  self: MethodSegment[B, R] =>

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] = callToTypeResponse()

}


abstract class GetSegment[R](queryParams: Map[String, Option[HttpParam]],
                             validAcceptHeaders: List[String],
                             req: RequestBuilder) extends MethodSegment[String, R] {

  val body = None

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    queryParameters = queryParameterMap,
    method = Get,
    validAcceptHeaders = validAcceptHeaders
  )

}

class StringGetSegment(queryParams: Map[String, Option[HttpParam]],
                       validAcceptHeaders: List[String],
                       req: RequestBuilder)
  extends GetSegment[String](queryParams, validAcceptHeaders, req)
  with StringResponseSegment[String]

class JsonGetSegment(queryParams: Map[String, Option[HttpParam]],
                     validAcceptHeaders: List[String],
                     req: RequestBuilder)
  extends GetSegment[JsValue](queryParams, validAcceptHeaders, req)
  with JsonResponseSegment[String]

class TypeGetSegment[R](queryParams: Map[String, Option[HttpParam]],
                        validAcceptHeaders: List[String],
                        req: RequestBuilder)
  extends GetSegment[R](queryParams, validAcceptHeaders, req)
  with TypeResponseSegment[String, R]


abstract class PutSegment[B, R](theBody: Option[B],
                                validAcceptHeaders: List[String],
                                validContentTypeHeaders: List[String],
                                req: RequestBuilder) extends MethodSegment[B, R] {

  val body = theBody

  protected val requestBuilder = req.copy(
    method = Put,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}

class StringPutSegment[B](theBody: Option[B],
                          validAcceptHeaders: List[String],
                          validContentTypeHeaders: List[String],
                          req: RequestBuilder)
  extends PutSegment[B, String](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with StringResponseSegment[B]

class JsonPutSegment[B](theBody: Option[B],
                        validAcceptHeaders: List[String],
                        validContentTypeHeaders: List[String],
                        req: RequestBuilder)
  extends PutSegment[B, JsValue](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with JsonResponseSegment[B]

class TypePutSegment[B, R](theBody: Option[B],
                           validAcceptHeaders: List[String],
                           validContentTypeHeaders: List[String],
                           req: RequestBuilder)
  extends PutSegment[B, R](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with TypeResponseSegment[B, R]


abstract class PostSegment[B, R](theBody: Option[B],
                                 formParams: Map[String, Option[HttpParam]],
                                 multipartParams: List[BodyPart],
                                 validAcceptHeaders: List[String],
                                 validContentTypeHeaders: List[String],
                                 req: RequestBuilder) extends MethodSegment[B, R] {

  val body = theBody

  protected val formParameterMap = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    method = Post,
    formParameters = formParameterMap,
    multipartParams = multipartParams,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}

class StringPostSegment[B](theBody: Option[B],
                           formParams: Map[String, Option[HttpParam]],
                           multipartParams: List[BodyPart],
                           validAcceptHeaders: List[String],
                           validContentTypeHeaders: List[String],
                           req: RequestBuilder)
  extends PostSegment[B, String](theBody, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req)
  with StringResponseSegment[B]

class JsonPostSegment[B](theBody: Option[B],
                         formParams: Map[String, Option[HttpParam]],
                         multipartParams: List[BodyPart],
                         validAcceptHeaders: List[String],
                         validContentTypeHeaders: List[String],
                         req: RequestBuilder)
  extends PostSegment[B, JsValue](theBody, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req)
  with JsonResponseSegment[B]

class TypePostSegment[B, R](theBody: Option[B],
                            formParams: Map[String, Option[HttpParam]],
                            multipartParams: List[BodyPart],
                            validAcceptHeaders: List[String],
                            validContentTypeHeaders: List[String],
                            req: RequestBuilder)
  extends PostSegment[B, R](theBody, formParams, multipartParams, validAcceptHeaders, validContentTypeHeaders, req)
  with TypeResponseSegment[B, R]


abstract class DeleteSegment[B, R](theBody: Option[B],
                                   validAcceptHeaders: List[String],
                                   validContentTypeHeaders: List[String],
                                   req: RequestBuilder) extends MethodSegment[B, R] {

  val body = theBody

  protected val requestBuilder = req.copy(
    method = Delete,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}

class StringDeleteSegment[B](theBody: Option[B],
                             validAcceptHeaders: List[String],
                             validContentTypeHeaders: List[String],
                             req: RequestBuilder)
  extends DeleteSegment[B, String](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with StringResponseSegment[B]

class JsonDeleteSegment[B](theBody: Option[B],
                           validAcceptHeaders: List[String],
                           validContentTypeHeaders: List[String],
                           req: RequestBuilder)
  extends DeleteSegment[B, JsValue](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with JsonResponseSegment[B]

class TypeDeleteSegment[B, R](theBody: Option[B],
                              validAcceptHeaders: List[String],
                              validContentTypeHeaders: List[String],
                              req: RequestBuilder)
  extends DeleteSegment[B, R](theBody, validAcceptHeaders, validContentTypeHeaders, req)
  with TypeResponseSegment[B, R]

