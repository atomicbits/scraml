package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

import java.util.{List => JList}

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io).
 *
 * It is not clear why formParameters is a Map from String to a List of FormParameters in the original Java model,
 * I would expect a Map from String to a signle FormParameter.
 *
 */
case class MimeType(mimeType: String, schema: String, formParameters: Map[String, List[Parameter]])

object MimeType {

  def apply(mimeType: org.raml.model.MimeType): MimeType = {

    val formParameters: Map[String, List[Parameter]] =
      Transformer
        .transformMap[JList[org.raml.model.parameter.FormParameter], List[Parameter]] { formParamList =>
        formParamList.asScala.toList.map(Parameter(_))
      }(mimeType.getFormParameters)


    MimeType(mimeType.getType, mimeType.getSchema, formParameters)
  }

}
