package io.atomicbits.scraml.dsl.support

import play.api.libs.json.Reads

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

class GetSegment(queryParams: Map[String, Option[String]],
                 validAcceptHeaders: List[String],
                 req: RequestBuilder) extends MethodSegment {

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    queryParameters = queryParameterMap,
    method = Get,
    validAcceptHeaders = validAcceptHeaders
  )

}

class PutSegment(body: String,
                 validAcceptHeaders: List[String],
                 validContentTypeHeaders: List[String],
                 req: RequestBuilder) extends MethodSegment {

  protected val requestBuilder = req.copy(
    method = Put,
    body = Option(body),
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}

class PostSegment(formParams: Map[String, Option[String]],
                  body: Option[String],
                  validAcceptHeaders: List[String],
                  validContentTypeHeaders: List[String],
                  req: RequestBuilder) extends MethodSegment {

  protected val formParameterMap = formParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    method = Post,
    formParameters = formParameterMap,
    body = body,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}

class DeleteSegment(body: Option[String],
                    validAcceptHeaders: List[String],
                    validContentTypeHeaders: List[String],
                    req: RequestBuilder) extends MethodSegment {

  protected val requestBuilder = req.copy(
    method = Delete,
    body = body,
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class ExecuteSegment[T](req: RequestBuilder) {

  def execute() = {
    println(s"request: $req")
    req.execute()
  }

  def executeToJson() = req.executeToJson()

  def executeToJsonDto()(implicit reader: Reads[T]) = req.executeToJsonDto()

}
