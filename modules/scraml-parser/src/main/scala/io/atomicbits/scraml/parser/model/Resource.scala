package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Resource(urlSegment: String,
                    urlParameter: Option[Parameter],
                    actions: List[Action],
                    resources: List[Resource]
                     )

object Resource {

  def apply(resource: org.raml.model.Resource): Resource = {

    val relativeUri = resource.getRelativeUri

    val uriParameters: Map[String, Parameter] =
      Transformer.transformMap[org.raml.model.parameter.UriParameter, Parameter](Parameter(_))(resource.getUriParameters)

    val oldActionsList: List[org.raml.model.Action] = resource.getActions.values().asScala.toSet.toList
    val newActionList = oldActionsList.map(a => Action(a))

    val subResources: List[Resource] =
      Transformer.transformMap[org.raml.model.Resource, Resource](Resource(_))(resource.getResources).values.toList

    /**
     * Resources in the Java RAML model can have relative URLs that consist of multiple segments,
     * e.g.: /rest/some/path/to/{param}/a/resource
     * Our DSL generation would benefit form a breakdown of this path into nested resources. The all resulting
     * resources would just be path elements to the last resource, which then contains the actions and sub
     * resources of the original resource.
     */

    /**
     * Breakdown of the url segments into nested resources.
     * @param urlSegments The URL segments.
     */
    def breakdownResourceUrl(urlSegments: List[String]): Resource = {

      def buildResourceSegment(segment: String): Resource = {
        if (segment.startsWith("{") && segment.endsWith("}")) {
          val pathParameterName = segment.stripPrefix("{").stripSuffix("}")
          val pathParameterMeta = uriParameters.get(pathParameterName)
          Resource(pathParameterName, pathParameterMeta, Nil, Nil)
        } else {
          Resource(segment, None, Nil, Nil)
        }
      }

      urlSegments match {
        case segment :: Nil => buildResourceSegment(segment).copy(actions = newActionList, resources = subResources)
        case segment :: segs => buildResourceSegment(segment).copy(resources = List(breakdownResourceUrl(segs)))
      }
    }

    // Make sure we can handle the root segment as wel
    if (relativeUri == "/")
      breakdownResourceUrl(Nil)
    else
      breakdownResourceUrl(relativeUri.split('/').toList.filter(!_.isEmpty))
  }

}
