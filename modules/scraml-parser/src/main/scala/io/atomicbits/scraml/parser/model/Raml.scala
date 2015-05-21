package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Raml(resources: List[Resource], schemas: Map[String, String])

object Raml {

  def apply(raml: org.raml.model.Raml): Raml = {
    val resources: List[Resource] = raml.getResources.values().asScala.toList.map(Resource(_))
    val schemas: Map[String, String] = {
      if (raml.getSchemas == null) Map.empty
      else {
        raml.getSchemas.asScala
          .foldLeft[Map[String, String]](Map.empty)((aggr, el) => aggr ++ el.asScala)
      }
    }
    Raml(resources, schemas)
  }

}

