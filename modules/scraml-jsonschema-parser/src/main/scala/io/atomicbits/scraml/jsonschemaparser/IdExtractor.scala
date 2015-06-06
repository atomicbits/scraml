package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object IdExtractor {

  def unapply(schema: JsObject): Option[Id] = {

    import IdAnalyser._

    val idType = (schema \ "id").asOpt[String] match {
      case Some(id) =>
        if (isRoot(id)) AbsoluteId(id = cleanRoot(id))
        else if (isFragment(id)) FragmentId(id)
        else RelativeId(id = id.trim.stripPrefix("/"))
      case None => ImplicitId
    }

    Option(idType)
  }

}

object RefExtractor {

  def unapply(schema: JsObject): Option[Id] = {

    import IdAnalyser._

    val idType = (schema \ "$ref").asOpt[String] match {
      case Some(id) =>
        if (isRoot(id)) AbsoluteId(id = cleanRoot(id))
        else if (isFragment(id)) FragmentId(id)
        else RelativeId(id = id.trim.stripPrefix("/"))
      case None => ImplicitId
    }

    Option(idType)
  }

}

object IdAnalyser {

  def isRoot(id: String): Boolean = id.contains("://")

  def isFragment(id: String): Boolean = {
    id.trim.split("#").length == 2 // this includes String objects that start with "#"
  }

  def cleanRoot(root: String): String = {
    root.trim.stripSuffix("#")
  }

  def isModelObject(schema: JsObject): Boolean = {

    (schema \ "type").asOpt[String].contains("object")
    // && (schema \ "properties").isInstanceOf[JsObject]
    // --> the above is unsafe because an object may not have a properties field, but a oneOf instead

  }

}


