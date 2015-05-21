package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Resource(relativeUri: String, uriParameters: Map[String, Parameter], actions: List[Action])

object Resource {

  def apply(resource: org.raml.model.Resource): Resource = {

    val relativeUri = resource.getRelativeUri

    val uriParameters: Map[String, Parameter] =
      Transformer.transformMap[org.raml.model.parameter.UriParameter, Parameter](Parameter(_))(resource.getUriParameters)

    val oldActionsList: List[org.raml.model.Action] = resource.getActions.values().asScala.toSet.toList
    val newActionList = oldActionsList.map(a => Action(a))

    Resource(relativeUri, uriParameters, newActionList)
  }

}