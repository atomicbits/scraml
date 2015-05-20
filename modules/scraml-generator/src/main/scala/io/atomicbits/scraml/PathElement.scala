package io.atomicbits.scraml

import scala.language.reflectiveCalls
import scala.concurrent.Future


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

  //  def execute[T](): Future[T] = ???
  def execute(): Unit = println(s"request: $req")

}

sealed trait Method

case object Get extends Method

case object Post extends Method

case object Put extends Method

case object Delete extends Method

case object Head extends Method

case object Opt extends Method

case object Patch extends Method

trait MediaTypeHeader {

  def mediaType: String

}

trait AcceptHeader extends MediaTypeHeader

trait ContentTypeHeader extends MediaTypeHeader

case class Request(protocol: String,
                   host: String,
                   port: Int,
                   reversePath: List[String] = Nil,
                   method: Method = Get,
                   queryParameters: Map[String, String] = Map.empty,
                   validAcceptHeaders: List[String] = Nil,
                   validContentTypeHeaders: List[String] = Nil,
                   headers: Map[String, String] = Map(),
                   body: Option[String] = None,
                   formatJsonResultBody: Boolean = false)
