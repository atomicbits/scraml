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

  assert(
    req.validAcceptHeaders.isEmpty || headers.get("Accept").exists(req.validAcceptHeaders.contains(_)),
    s"""no valid Accept header is given for this resource:
       |valid Accept headers are: ${req.validAcceptHeaders.mkString(", ")}
     """.stripMargin
  )

  assert(
    req.validContentTypeHeaders.isEmpty || headers.get("Content-Type").exists(req.validContentTypeHeaders.contains(_)),
    s"""no valid Content-Type header is given for this resource:
       |valid Content-Type headers are: ${req.validContentTypeHeaders.mkString(", ")}
     """.stripMargin
  )

  protected val requestBuilder = req.copy(headers = headers)

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


class FormatJsonSegment(req: RequestBuilder) extends Segment {

  protected val requestBuilder = req.copy(formatJsonResultBody = true)

}


class ExecuteSegment(req: RequestBuilder) {

  def execute() = {
    println(s"request: $req")
    req.execute()
  }

  def executeToJson() = req.executeToJson()

  def executeToJsonDto[T]()(implicit reader: Reads[T]) = req.executeToJsonDto()

}
