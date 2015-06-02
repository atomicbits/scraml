package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object IdExtractor {

  def unapply(schema: JsObject): Option[IdType] = {

    val idType = (schema \ "id").asOpt[String] match {
      case Some(id) =>
        if (isRoot(id)) Root(id = cleanRoot(id))
        else Relative(id = id.trim.stripPrefix("/"))
      case None =>
        if (isModelObject(schema))
          throw new IllegalArgumentException(s"Anonymous object reference found in JSON schema: $schema")
        else NoId
    }

    Option(idType)
  }

  def isRoot(id: String): Boolean = id.contains("://")

  def cleanRoot(root: String): String = {
    root.trim.stripSuffix("#")
  }

  def isModelObject(schema: JsObject): Boolean = {

    (schema \ "type").asOpt[String].exists { typeValue =>
      typeValue == "object" && (schema \ "properties").isInstanceOf[JsObject]
    }

  }

}
