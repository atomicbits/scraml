package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class MimeType(mimeType: String, schema: String)

object MimeType {

  def apply(mimeType: org.raml.model.MimeType): MimeType = {
    MimeType(mimeType.getType, mimeType.getSchema)
  }

}
