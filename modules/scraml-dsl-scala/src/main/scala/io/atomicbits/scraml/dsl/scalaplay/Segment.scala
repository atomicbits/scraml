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

import play.api.libs.json.{ Format, JsValue }

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

// ToDo: rename to MethodSegment to Request (which is what it is)
abstract class MethodSegment[B, R](method: Method,
                                   theBody: Option[B],
                                   queryParams: Map[String, Option[HttpParam]],
                                   queryString: Option[TypedQueryParams],
                                   formParams: Map[String, Option[HttpParam]],
                                   multipartParams: List[BodyPart],
                                   binaryBody: Option[BinaryRequest] = None,
                                   expectedAcceptHeader: Option[String],
                                   expectedContentTypeHeader: Option[String],
                                   req: RequestBuilder)
    extends Segment {

  val body = theBody

  protected val queryParameterMap: Map[String, HttpParam] =
    queryString
      .map(_.params)
      .getOrElse {
        queryParams.collect { case (key, Some(value)) => (key, value) }
      }

  protected val formParameterMap: Map[String, HttpParam] = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val _requestBuilder: RequestBuilder = {
    val reqUpdated =
      req.copy(
        method          = method,
        queryParameters = queryParameterMap,
        formParameters  = formParameterMap,
        multipartParams = multipartParams,
        binaryBody      = binaryBody
      )

    val reqWithAccept =
      expectedAcceptHeader map { acceptHeader =>
        if (reqUpdated.headers.get("Accept").isEmpty)
          reqUpdated.copy(headers = reqUpdated.headers + ("Accept" -> acceptHeader))
        else reqUpdated
      } getOrElse reqUpdated

    val reqWithContentType =
      expectedContentTypeHeader map { contentHeader =>
        if (reqWithAccept.headers.get("Content-Type").isEmpty)
          reqWithAccept.copy(headers = reqWithAccept.headers + ("Content-Type" -> contentHeader))
        else reqWithAccept
      } getOrElse reqWithAccept

    /**
      * add default request charset if necessary
      *
      * see https://www.w3.org/Protocols/rfc1341/4_Content-Type.html
      * charset is case-insensitive:
      * * http://stackoverflow.com/questions/7718476/are-http-headers-content-type-c-case-sensitive
      * * https://www.w3.org/TR/html4/charset.html#h-5.2.1
      */
    val reqWithRequestCharset =
      reqWithContentType.headers.get("Content-Type").map { headerValues =>
        val hasCharset = !headerValues.exists(_.toLowerCase.contains("charset"))
        val isBinary   = headerValues.exists(_.toLowerCase.contains("octet-stream"))
        if (hasCharset && !isBinary && headerValues.nonEmpty) {
          val newFirstHeaderValue = s"${headerValues.head}; charset=${reqWithContentType.client.config.requestCharset.name}"
          val updatedHeaders      = reqWithContentType.headers setMany ("Content-Type", newFirstHeaderValue :: headerValues.tail)
          reqWithContentType.copy(headers = updatedHeaders)
        } else reqWithContentType
      } getOrElse reqWithContentType

    reqWithRequestCharset
  }

  def isFormUrlEncoded: Boolean =
    _requestBuilder.allHeaders
      .get("Content-Type")
      .exists { values =>
        values.exists(value => value.contains("application/x-www-form-urlencoded"))
      }

  def jsonBodyToString()(implicit bodyFormat: Format[B]): (RequestBuilder, Option[String]) = {
    if (formParams.isEmpty && body.isDefined && isFormUrlEncoded) {
      val formPs: Map[String, HttpParam] = HttpParam.toFormUrlEncoded(bodyFormat.writes(body.get))
      val reqBuilder                     = _requestBuilder.copy(formParameters = formPs)
      (reqBuilder, None)
    } else {
      val bodyToSend = body.map(bodyFormat.writes(_).toString())
      (_requestBuilder, bodyToSend)
    }
  }

}

class StringMethodSegment[B](method: Method,
                             theBody: Option[B] = None,
                             queryParams: Map[String, Option[HttpParam]],
                             queryString: Option[TypedQueryParams]      = None,
                             formParams: Map[String, Option[HttpParam]] = Map.empty,
                             multipartParams: List[BodyPart]            = List.empty,
                             binaryParam: Option[BinaryRequest]         = None,
                             expectedAcceptHeader: Option[String]       = None,
                             expectedContentTypeHeader: Option[String]  = None,
                             req: RequestBuilder)
    extends MethodSegment[B, String](method,
                                     theBody,
                                     queryParams,
                                     queryString,
                                     formParams,
                                     multipartParams,
                                     binaryParam,
                                     expectedAcceptHeader,
                                     expectedContentTypeHeader,
                                     req) {

  def callWithPrimitiveBody(): Future[Response[String]] = {
    val bodyToSend = body.map(_.toString())
    _requestBuilder.callToStringResponse(bodyToSend)
  }

  def call()(implicit bodyFormat: Format[B]): Future[Response[String]] = {
    val (reqBuilder, preparedBody) = jsonBodyToString()
    reqBuilder.callToStringResponse(preparedBody)
  }

}

class JsonMethodSegment[B](method: Method,
                           theBody: Option[B] = None,
                           queryParams: Map[String, Option[HttpParam]],
                           queryString: Option[TypedQueryParams]      = None,
                           formParams: Map[String, Option[HttpParam]] = Map.empty,
                           multipartParams: List[BodyPart]            = List.empty,
                           binaryParam: Option[BinaryRequest]         = None,
                           expectedAcceptHeader: Option[String]       = None,
                           expectedContentTypeHeader: Option[String]  = None,
                           req: RequestBuilder)
    extends MethodSegment[B, JsValue](method,
                                      theBody,
                                      queryParams,
                                      queryString,
                                      formParams,
                                      multipartParams,
                                      binaryParam,
                                      expectedAcceptHeader,
                                      expectedContentTypeHeader,
                                      req) {

  def callWithPrimitiveBody(): Future[Response[JsValue]] = {
    val bodyToSend = body.map(_.toString())
    _requestBuilder.callToJsonResponse(bodyToSend)
  }

  def call()(implicit bodyFormat: Format[B]): Future[Response[JsValue]] = {
    val (reqBuilder, preparedBody) = jsonBodyToString()
    reqBuilder.callToJsonResponse(preparedBody)
  }

}

class TypeMethodSegment[B, R](method: Method,
                              theBody: Option[B]                          = None,
                              queryParams: Map[String, Option[HttpParam]] = Map.empty,
                              queryString: Option[TypedQueryParams]       = None,
                              formParams: Map[String, Option[HttpParam]]  = Map.empty,
                              multipartParams: List[BodyPart]             = List.empty,
                              binaryParam: Option[BinaryRequest]          = None,
                              expectedAcceptHeader: Option[String]        = None,
                              expectedContentTypeHeader: Option[String]   = None,
                              req: RequestBuilder)
    extends MethodSegment[B, R](method,
                                theBody,
                                queryParams,
                                queryString,
                                formParams,
                                multipartParams,
                                binaryParam,
                                expectedAcceptHeader,
                                expectedContentTypeHeader,
                                req) {

  def callWithPrimitiveBody()(implicit responseFormat: Format[R]): Future[Response[R]] = {
    val bodyToSend = body.map(_.toString())
    _requestBuilder.callToTypeResponse[R](bodyToSend)
  }

  def call()(implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] = {
    val (reqBuilder, preparedBody) = jsonBodyToString()
    reqBuilder.callToTypeResponse(preparedBody)
  }

}

class BinaryMethodSegment[B](method: Method,
                             theBody: Option[B] = None,
                             queryParams: Map[String, Option[HttpParam]],
                             queryString: Option[TypedQueryParams]      = None,
                             formParams: Map[String, Option[HttpParam]] = Map.empty,
                             multipartParams: List[BodyPart]            = List.empty,
                             binaryParam: Option[BinaryRequest]         = None,
                             expectedAcceptHeader: Option[String]       = None,
                             expectedContentTypeHeader: Option[String]  = None,
                             req: RequestBuilder)
    extends MethodSegment[B, BinaryData](method,
                                         theBody,
                                         queryParams,
                                         queryString,
                                         formParams,
                                         multipartParams,
                                         binaryParam,
                                         expectedAcceptHeader,
                                         expectedContentTypeHeader,
                                         req) {

  def callWithPrimitiveBody(): Future[Response[BinaryData]] = {
    val bodyToSend = body.map(_.toString())
    _requestBuilder.callToBinaryResponse(bodyToSend)
  }

  def call()(implicit bodyFormat: Format[B]): Future[Response[BinaryData]] = {
    val (reqBuilder, preparedBody) = jsonBodyToString()
    reqBuilder.callToBinaryResponse(preparedBody)
  }

}
