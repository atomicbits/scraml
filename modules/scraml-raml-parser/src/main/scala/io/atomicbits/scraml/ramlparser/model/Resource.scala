/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedString
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{ JsObject, Json }

import scala.util.Try
import io.atomicbits.scraml.util.TryUtils._
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.language.postfixOps

/**
  * Created by peter on 10/02/16.
  */
case class Resource(urlSegment: String,
                    urlParameter: Option[Parameter] = None,
                    displayName: Option[String]     = None,
                    description: Option[String]     = None,
                    actions: List[Action]           = List.empty,
                    resources: List[Resource]       = List.empty,
                    parent: Option[Resource]        = None) {

  lazy val resourceMap: Map[String, Resource] = resources.map(resource => resource.urlSegment -> resource).toMap

  lazy val actionMap: Map[Method, Action] = actions.map(action => action.actionType -> action).toMap

}

object Resource {

  def apply(resourceUrl: String, jsObject: JsObject)(implicit parseContext: ParseContext): Try[Resource] = {

    // Make sure we can handle the root segment as wel
    val urlSegments: List[String] = {
      if (resourceUrl == "/")
        Nil
      else
        resourceUrl.split('/').toList.filter(!_.isEmpty)
    }

    parseContext.withSourceAndUrlSegments(jsObject, urlSegments) {

      // Apply the listed traits to all methods in the resource.
      //
      // From the spec:
      // "A trait can also be applied to a resource by using the is node. Using this node is equivalent to applying the trait to
      // all methods for that resource, whether declared explicitly in the resource definition or inherited from a resource type."
      //
      // This must be done *before* calling the child resources
      // recursively to adhere to the trait priority as described in:
      // https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/#algorithm-of-merging-traits-and-methods
      parseContext.resourceTypes.applyToResource(jsObject) { resourceJsObj =>
        // Actions

        val tryMethods: Seq[(Method, Try[JsObject])] =
          resourceJsObj.fields
            .collect {
              case (Method(method), jsObj: JsObject) => (method, jsObj)
              case (Method(method), _)               => (method, Json.obj())
            }
            .map {
              case (meth, jsOb) =>
                val actionOwnTraits      = parseContext.traits.mergeInToAction(jsOb)
                val actionResourceTraits = actionOwnTraits.flatMap(parseContext.traits.mergeInToActionFromResource(_, resourceJsObj))
                (meth, actionResourceTraits)
            }
            .toSeq

        val accumulated: Try[Map[Method, JsObject]] = accumulate(tryMethods.toMap)
        val actionSeq: Try[Seq[Try[Action]]]        = accumulated.map(methodMap => methodMap.map(Action(_)).toSeq)
        val actions: Try[Seq[Action]]               = actionSeq.flatMap(accumulate(_))

        // Subresources

        val subResourceMap =
          resourceJsObj.value.toMap.collect {
            case (fieldName, jsOb: JsObject) if fieldName.startsWith("/") => Resource(fieldName, jsOb)
          } toSeq

        val subResources: Try[Seq[Resource]] = accumulate(subResourceMap)

        val displayName: Try[Option[String]] = Try(resourceJsObj.fieldStringValue("displayName"))

        val description: Try[Option[String]] = Try(resourceJsObj.fieldStringValue("description"))

        // URI parameters
        val uriParameterMap: Try[Parameters] = Parameters((resourceJsObj \ "uriParameters").toOption)

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
                           uriParamMap: Parameters,
                           actionSeq: Seq[Action],
                           childResources: Seq[Resource]): Resource = {

          def buildResourceSegment(segment: String): Resource = {
            if (segment.startsWith("{") && segment.endsWith("}")) {
              val pathParameterName = segment.stripPrefix("{").stripSuffix("}")
              val pathParameterMeta =
                uriParamMap
                  .byName(pathParameterName)
                  .getOrElse(Parameter(pathParameterName, TypeRepresentation(new ParsedString()), required = true))
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

          breakdownResourceUrl(urlSegments)
        }

        withSuccess(displayName, description, uriParameterMap, actions, subResources)(createResource)
      }

    }

  }

}
