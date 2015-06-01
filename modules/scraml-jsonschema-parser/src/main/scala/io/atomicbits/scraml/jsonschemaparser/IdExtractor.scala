package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object IdExtractor {

  def unapply(schema: JsObject): Option[IdType] = {

    val idType = (schema \ "id").asOpt[String] match {
      case Some(id) =>
        if (isRoot(id)) Root(id = cleanRoot(id), anchorFromRoot(id))
        else Relative(id = id.trim)
      case None => NoId
    }

    Option(idType)
  }

  def isRoot(id: String): Boolean = id.contains("://")

  def cleanRoot(root: String): String = {
    root.trim.stripSuffix("#")
  }

  def anchorFromRoot(root: String): String = {
    root.split('/').toList.dropRight(1).mkString("/")
  }

}
