package io.atomicbits.scraml.generator.path

import play.api.libs.json.{Reads, JsValue}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.language.reflectiveCalls


sealed trait PathElement {

  protected def request: Request

}

class PlainPathElement(pathElement: String, req: Request) extends PathElement {

  protected val request = req.copy(reversePath = pathElement :: req.reversePath)
}

class StringPathElement(value: String, req: Request) extends PathElement {

  protected val request = req.copy(reversePath = value :: req.reversePath)
}

class IntPathelement(value: Int, req: Request) extends PathElement {

  protected val request = req.copy(reversePath = value.toString :: req.reversePath)
}

class DoublePathelement(value: Double, req: Request) extends PathElement {

  protected val request = req.copy(reversePath = value.toString :: req.reversePath)
}

class BooleanPathelement(value: Boolean, req: Request) extends PathElement {

  protected val request = req.copy(reversePath = value.toString :: req.reversePath)
}

class HeaderPathElement(headers: Map[String, String], req: Request) extends PathElement {

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

  protected val request = req.copy(headers = headers)

}

sealed trait MethodPathElement extends PathElement

class GetPathElement(queryParams: Map[String, Option[String]],
                     validAcceptHeaders: List[String],
                     req: Request) extends MethodPathElement {

  protected val queryParameterMap = queryParams.collect { case (key, Some(value)) => (key, value) }

  protected val request = req.copy(
    queryParameters = queryParameterMap,
    method = Get,
    validAcceptHeaders = validAcceptHeaders
  )

}

class PutPathElement(body: String,
                     validAcceptHeaders: List[String],
                     validContentTypeHeaders: List[String],
                     req: Request) extends MethodPathElement {

  protected val request = req.copy(
    method = Put,
    body = Option(body),
    validAcceptHeaders = validAcceptHeaders,
    validContentTypeHeaders = validContentTypeHeaders
  )

}


class FormatJsonPathElement(req: Request) extends PathElement {

  protected val request = req.copy(formatJsonResultBody = true)

}


class ExecutePathElement(req: Request) {

  def execute() = {
    println(s"request: $req")
    req.execute()
  }

  def executeToJson() = req.executeToJson()

  def executeToJsonDto[T]()(implicit reader: Reads[T]) = req.executeToJsonDto()

}
