package io.atomicbits.scraml.dsl.support

import play.api.libs.json.Reads

import scala.language.reflectiveCalls


sealed trait PathElement {

  protected def requestBuilder: RequestBuilder

}

class PlainPathElement(pathElement: String, req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(reversePath = pathElement :: req.reversePath)
}

class StringPathElement(value: String, req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(reversePath = value :: req.reversePath)
}

class IntPathelement(value: Int, req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(reversePath = value.toString :: req.reversePath)
}

class DoublePathelement(value: Double, req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(reversePath = value.toString :: req.reversePath)
}

class BooleanPathelement(value: Boolean, req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(reversePath = value.toString :: req.reversePath)
}

class HeaderPathElement(headers: Map[String, String], req: RequestBuilder) extends PathElement {

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

sealed trait MethodPathElement extends PathElement

class GetPathElement(queryParams: Map[String, Option[String]],
                     validAcceptHeaders: List[String],
                     req: RequestBuilder) extends MethodPathElement {

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val requestBuilder = req.copy(
    queryParameters = queryParameterMap,
    method = Get,
    validAcceptHeaders = validAcceptHeaders
  )

}

class PutPathElement(body: String,
                     validAcceptHeaders: List[String],
                     validContentTypeHeaders: List[String],
                     req: RequestBuilder) extends MethodPathElement {

  protected val requestBuilder = req.copy(
    method = Put,
    body = Option(body),
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class FormatJsonPathElement(req: RequestBuilder) extends PathElement {

  protected val requestBuilder = req.copy(formatJsonResultBody = true)

}


class ExecutePathElement(req: RequestBuilder) {

  def execute() = {
    println(s"request: $req")
    req.execute()
  }

  def executeToJson() = req.executeToJson()

  def executeToJsonDto[T]()(implicit reader: Reads[T]) = req.executeToJsonDto()

}
