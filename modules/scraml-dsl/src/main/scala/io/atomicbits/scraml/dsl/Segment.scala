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

import play.api.libs.json.Format

import scala.language.reflectiveCalls


sealed trait Segment {

  protected def requestBuilder: RequestBuilder

}

class PlainSegment(pathElement: String, req: RequestBuilder) extends Segment {

  protected val requestBuilder = req.copy(reversePath = pathElement :: req.reversePath)
}

class ParamSegment[T](value: T, req: RequestBuilder) extends Segment {

  protected val requestBuilder = req.copy(reversePath = value.toString :: req.reversePath)
}

class HeaderSegment(headers: Map[String, String], req: RequestBuilder) extends Segment {

  protected val requestBuilder = {

    if (req.isFormPost && (req.defaultHeaders ++ headers).get("Content-Type").isEmpty) {
      val headersWithAddedContentType = headers.updated("Content-Type", "application/x-www-form-urlencoded")
      req.copy(headers = headersWithAddedContentType)
    } else {
      req.copy(headers = headers)
    }
  }

  assert(
    req.validAcceptHeaders.isEmpty ||
      requestBuilder.allHeaders.get("Accept").exists(req.validAcceptHeaders.contains(_)),
    s"""no valid Accept header is given for this resource:
       |valid Accept headers are: ${req.validAcceptHeaders.mkString(", ")}
     """.stripMargin
  )

  assert(
    req.validContentTypeHeaders.isEmpty ||
      requestBuilder.allHeaders.get("Content-Type").exists(req.validContentTypeHeaders.contains(_)),
    s"""no valid Content-Type header is given for this resource:
       |valid Content-Type headers are: ${req.validContentTypeHeaders.mkString(", ")}
     """.stripMargin
  )

}

sealed trait MethodSegment extends Segment


class GetSegment(queryParams: Map[String, Option[HttpParam]],
                 validAcceptHeaders: List[String],
                 req: RequestBuilder) extends MethodSegment {

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    queryParameters = queryParameterMap,
    method = Get,
    validAcceptHeaders = validAcceptHeaders
  )

}


class PutSegment(validAcceptHeaders: List[String],
                 validContentTypeHeaders: List[String],
                 req: RequestBuilder) extends MethodSegment {

  protected val requestBuilder = req.copy(
    method = Put,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class PostSegment(formParams: Map[String, Option[HttpParam]],
                  multipartParams: List[BodyPart],
                  validAcceptHeaders: List[String],
                  validContentTypeHeaders: List[String],
                  req: RequestBuilder) extends MethodSegment {

  protected val formParameterMap = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    method = Post,
    formParameters = formParameterMap,
    multipartParams = multipartParams,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class DeleteSegment(validAcceptHeaders: List[String],
                    validContentTypeHeaders: List[String],
                    req: RequestBuilder) extends MethodSegment {

  protected val requestBuilder = req.copy(
    method = Delete,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


// The ExecuteSegment is necessary because the RequestBuilder is untyped and cannot hold the typed body.
class ExecuteSegment[B, R](req: RequestBuilder, body: Option[B]) {

  def callToStringResponse()(implicit bodyFormat: Format[B]) = req.callToStringResponse[B](body)

  def callToJsonResponse()(implicit bodyFormat: Format[B]) = req.callToJsonResponse[B](body)

  def callToTypeResponse()(implicit bodyFormat: Format[B], responseFormat: Format[R]) = req.callToTypeResponse[B, R](body)

}
