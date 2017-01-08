/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedParameter, ParsedParameters, ParsedString }
import io.atomicbits.scraml.ramlparser.parser.{ KeyedList, ParseContext, RamlParseException }
import play.api.libs.json.{ JsArray, JsObject }

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.util.TryUtils._
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.language.postfixOps

/**
  * Created by peter on 10/02/16.
  */
case class Resource(urlSegment: String,
                    urlParameter: Option[ParsedParameter] = None,
                    displayName: Option[String]           = None,
                    description: Option[String]           = None,
                    actions: List[Action]                 = List.empty,
                    resources: List[Resource]             = List.empty,
                    parent: Option[Resource]              = None) {

  lazy val resourceMap: Map[String, Resource] = resources.map(resource => resource.urlSegment -> resource).toMap

  lazy val actionMap: Map[Method, Action] = actions.map(action => action.actionType -> action).toMap

}

object Resource {

  def apply(resourceUrl: String, jsObject: JsObject)(implicit parseContext: ParseContext): Try[Resource] = {

    parseContext.withSource(jsObject) {

      // Apply all traits to the resource. This must be done *before* calling the child resources
      // recursively to adhere to the trait priority as described in:
      // https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/#algorithm-of-merging-traits-and-methods
      parseContext.traits.applyTo(jsObject) { jsObj =>
        val displayName: Try[Option[String]] = Try(jsObj.fieldStringValue("displayName"))

        val description: Try[Option[String]] = Try(jsObj.fieldStringValue("description"))

        // URI parameters
        val uriParameterMap: Try[ParsedParameters] = ParsedParameters((jsObj \ "uriParameters").toOption)

        // Actions

        val tryActions =
          jsObj.fields.collect {
            case Action(action) => action
          }

        val actions = accumulate(tryActions)

        // Subresources

        val subResourceMap =
          jsObj.value.toMap.collect {
            case (fieldName, jsOb: JsObject) if fieldName.startsWith("/") => Resource(fieldName, jsOb)
          } toSeq

        val subResources: Try[Seq[Resource]] = accumulate(subResourceMap)

        /**
          * Resources in the Java RAML model can have relative URLs that consist of multiple segments in a single Resource,
          * e.g.: /rest/some/path/to/{param}/a/resource
          * Our DSL generation would benefit form a breakdown of this path into nested resources. The all resulting
          * resources would just be path elements to the last resource, which then contains the actions and sub
          * resources of the original resource.
          *
          * Breakdown of the url segments into nested resources.
          */
        def createResource(displayN: Option[String],
                           desc: Option[String],
                           uriParamMap: ParsedParameters,
                           actionSeq: Seq[Action],
                           childResources: Seq[Resource]): Resource = {

          def buildResourceSegment(segment: String): Resource = {
            if (segment.startsWith("{") && segment.endsWith("}")) {
              val pathParameterName = segment.stripPrefix("{").stripSuffix("}")
              val pathParameterMeta =
                uriParamMap
                  .byName(pathParameterName)
                  .getOrElse(ParsedParameter(pathParameterName, TypeRepresentation(new ParsedString()), true, false))
              Resource(
                urlSegment   = pathParameterName,
                urlParameter = Some(pathParameterMeta)
              )
            } else {
              Resource(
                urlSegment = segment
              )
            }
          }

          def connectParentChildren(parent: Resource, children: List[Resource]): Resource = {
            val childrenWithUpdatedParent = children.map(_.copy(parent = Some(parent)))
            parent.copy(resources = childrenWithUpdatedParent)
          }

          def breakdownResourceUrl(segments: List[String]): Resource = {
            segments match {
              case segment :: Nil =>
                val resource: Resource          = buildResourceSegment(segment)
                val connectedResource: Resource = connectParentChildren(resource, childResources.toList)
                connectedResource.copy(actions = actionSeq.toList)
              case segment :: segs =>
                val resource: Resource = buildResourceSegment(segment)
                connectParentChildren(resource, List(breakdownResourceUrl(segs)))
              case Nil =>
                val resource: Resource          = buildResourceSegment("") // Root segment.
                val connectedResource: Resource = connectParentChildren(resource, childResources.toList)
                connectedResource.copy(actions = actionSeq.toList)
            }
          }

          // Make sure we can handle the root segment as wel
          val urlSegments = {
            if (resourceUrl == "/")
              Nil
            else
              resourceUrl.split('/').toList.filter(!_.isEmpty)
          }

          breakdownResourceUrl(urlSegments)
        }

        withSuccess(displayName, description, uriParameterMap, actions, subResources)(createResource)
      }

    }

  }

}
