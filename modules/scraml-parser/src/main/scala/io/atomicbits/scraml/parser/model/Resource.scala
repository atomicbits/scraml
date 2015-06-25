/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

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

    val allSubResources: List[Resource] =
      Transformer.transformMap[org.raml.model.Resource, Resource](Resource(_))(resource.getResources).values.toList

    val (emptySubResources, nonEmptySubResources) = allSubResources.partition(_.urlSegment.isEmpty)

    val actionList = newActionList ++ emptySubResources.flatMap(_.actions)

    // Group all subresources with the same urlSegment and urlParameter
    val groupedSubResources: List[List[Resource]] =
      nonEmptySubResources.groupBy(resource => (resource.urlSegment, resource.urlParameter)).values.toList

    // Merge all actions and subresources of all resources that have the same (urlSegment, urlParameter)
    def mergeResources(resources: List[Resource]): Resource = {
      resources.reduce { (resourceA, resourceB) =>
        resourceA.copy(actions = resourceA.actions ++ resourceB.actions, resources = resourceA.resources ++ resourceB.resources)
      }
    }
    val subResources = groupedSubResources.map(mergeResources)

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
        case segment :: Nil  => buildResourceSegment(segment).copy(actions = actionList, resources = subResources)
        case segment :: segs => buildResourceSegment(segment).copy(resources = List(breakdownResourceUrl(segs)))
        // Todo: handle the case Nil without introducing an extra 'root' path
        case Nil => buildResourceSegment("").copy(actions = actionList, resources = subResources)
      }
    }

    // Make sure we can handle the root segment as wel
    if (relativeUri == "/")
      breakdownResourceUrl(Nil)
    else
      breakdownResourceUrl(relativeUri.split('/').toList.filter(!_.isEmpty))
  }

}
