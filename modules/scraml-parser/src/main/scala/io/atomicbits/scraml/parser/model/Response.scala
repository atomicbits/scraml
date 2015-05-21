package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Response(headers: Map[String, Parameter], body: Map[String, MimeType])

object Response {

  def apply(response: org.raml.model.Response): Response = {

    val headers: Map[String, Parameter] =
      Transformer.transformMap[org.raml.model.parameter.Header, Parameter](Parameter(_))(response.getHeaders)

    val body: Map[String, MimeType] =
      Transformer.transformMap[org.raml.model.MimeType, MimeType](MimeType(_))(response.getBody)

    Response(headers, body)
  }

}
