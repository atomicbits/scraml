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

import io.atomicbits.scraml.ramlparser.lookup._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.Types
import io.atomicbits.scraml.ramlparser.parser.{ ParseContext, RamlParseException }
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.util.TryUtils._

/**
  * Created by peter on 10/02/16.
  */
case class Raml(title: String,
                mediaType: Option[MediaType],
                description: Option[String],
                version: Option[String],
                baseUri: Option[String],
                baseUriParameters: Parameters,
                protocols: Option[Seq[String]],
                traits: Traits,
                types: Types,
                resources: List[Resource]) {

  def collectCanonicals(defaultBasePath: List[String]): (Raml, CanonicalLookup) = {
    implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
    val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)
    canonicalTypeCollector.collect(this)
  }

  lazy val resourceMap: Map[String, Resource] = resources.map(resource => resource.urlSegment -> resource).toMap

}

object Raml {

  def apply(ramlJson: JsObject)(parseCtxt: ParseContext): Try[Raml] = {

    val tryTraits: Try[Traits] =
      (ramlJson \ "traits").toOption.map(Traits(_)(parseCtxt)).getOrElse(Success(Traits()))

    val tryResourceTypes: Try[ResourceTypes] =
      (ramlJson \ "resourceTypes").toOption.map(ResourceTypes(_)(parseCtxt)).getOrElse(Success(ResourceTypes()))

    val mediaType: Try[Option[MediaType]] = {
      (ramlJson \ "mediaType").toOption.collect {
        case JsString(mType) => Success(Option(MediaType(mType)))
        case x               => Failure(RamlParseException(s"The mediaType in ${parseCtxt.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    implicit val parseContext: ParseContext = {

      val tryParseCtxt =
        for {
          resourceTypes <- tryResourceTypes
          traits <- tryTraits
          defaultMediaType <- mediaType
        } yield parseCtxt.copy(resourceTypes = resourceTypes, traits = traits, defaultMediaType = defaultMediaType)

      tryParseCtxt match {
        case Success(ctxt) => ctxt
        case Failure(exc)  => sys.error(s"Parse error: ${exc.getMessage}.")
      }
    }

    val title: Try[String] =
      (ramlJson \ "title").toOption.collect {
        case JsString(t) => Success(t)
        case x =>
          Failure(RamlParseException(s"File ${parseCtxt.sourceTrail} has a title field that is not a string value."))
      } getOrElse Failure(RamlParseException(s"File ${parseCtxt.sourceTrail} does not contain the mandatory title field."))

    val types: Try[Types] = {
      val x = List((ramlJson \ "types").toOption, (ramlJson \ "schemas").toOption).flatten
      (x: @unchecked) match {
        case List(ts, ss) =>
          Failure(
            RamlParseException(
              s"File ${parseCtxt.sourceTrail} contains both a 'types' and a 'schemas' field. You should only use a 'types' field."
            )
          )
        case List(t) => Types(t)
        case Nil     => Success(Types())
      }
    }

    val description: Try[Option[String]] = {
      (ramlJson \ "description").toOption.collect {
        case JsString(docu) => Success(Option(docu))
        case x =>
          Failure(RamlParseException(s"The description field in ${parseCtxt.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    val protocols: Try[Option[Seq[String]]] = {

      def toProtocolString(protocolString: JsValue): Try[String] = {
        protocolString match {
          case JsString(pString) if pString.toUpperCase == "HTTP"  => Success("HTTP")
          case JsString(pString) if pString.toUpperCase == "HTTPS" => Success("HTTPS")
          case JsString(pString) =>
            Failure(RamlParseException(s"The protocols in ${parseCtxt.sourceTrail} should be either HTTP or HTTPS."))
          case x =>
            Failure(RamlParseException(s"At least one of the protocols in ${parseCtxt.sourceTrail} is not a string value."))
        }
      }

      (ramlJson \ "protocols").toOption.collect {
        case JsArray(pcols) => TryUtils.accumulate(pcols.toSeq.map(toProtocolString)).map(Some(_))
        case x =>
          Failure(RamlParseException(s"The protocols field in ${parseCtxt.sourceTrail} must be an array of string values."))
      }.getOrElse(Success(None))
    }

    val version: Try[Option[String]] = {
      (ramlJson \ "version").toOption.collect {
        case JsString(v) => Success(Option(v))
        case JsNumber(v) => Success(Option(v.toString()))
        case x =>
          Failure(RamlParseException(s"The version field in ${parseCtxt.sourceTrail} must be a string or a number value."))
      }.getOrElse(Success(None))
    }

    val baseUri: Try[Option[String]] = {
      (ramlJson \ "baseUri").toOption.collect {
        case JsString(v) => Success(Option(v))
        case x =>
          Failure(RamlParseException(s"The baseUri field in ${parseCtxt.sourceTrail} must be a string value."))
      }.getOrElse(Success(None))
    }

    val baseUriParameters: Try[Parameters] = Parameters((ramlJson \ "baseUriParameters").toOption)

    /**
      * According to the specs on https://github.com/raml-org/raml-spec/blob/raml-10/versions/raml-10/raml-10.md#scalar-type-specialization
      *
      * "The resources of the API, identified as relative URIs that begin with a slash (/). Every property whose key begins with a
      * slash (/), and is either at the root of the API definition or is the child property of a resource property, is a resource
      * property, e.g.: /users, /{groupId}, etc."
      *
      */
    val resources: Try[List[Resource]] = {

      val resourceFields: List[Try[Resource]] =
        ramlJson.fieldSet.collect {
          case (field, jsObject: JsObject) if field.startsWith("/") => Resource(field, jsObject)
        }.toList

      TryUtils.accumulate(resourceFields).map(_.toList).map(unparallellizeResources(_, None))
    }

    //    val resourceTypes: Try[]
    //    val annotationTypes: Try[]

    /**
      * title
      * traits
      * types (schemas - deprecated)
      * mediaType
      * description
      * protocols
      * version
      * baseUri
      * baseUriParameters
      *
      * resourceTypes
      * annotationTypes
      * securedBy
      * securitySchemes
      * documentation
      * uses
      */
    withSuccess(
      title,
      mediaType,
      description,
      version,
      baseUri,
      baseUriParameters,
      protocols,
      tryTraits,
      types,
      resources
    )(Raml(_, _, _, _, _, _, _, _, _, _))

  }

  private def unparallellizeResources(resources: List[Resource], parent: Option[Resource] = None): List[Resource] = {

    // Merge all actions and subresources of all resources that have the same (urlSegment, urlParameter)
    def mergeResources(resources: List[Resource]): Resource = {
      resources.reduce { (resourceA, resourceB) =>
        val descriptionChoice = List(resourceA.description, resourceB.description).flatten.headOption
        val displayNameChoice = List(resourceA.displayName, resourceB.displayName).flatten.headOption
        resourceA.copy(
          description = descriptionChoice,
          displayName = displayNameChoice,
          actions     = resourceA.actions ++ resourceB.actions,
          resources   = resourceA.resources ++ resourceB.resources
        )
      }
    }

    // All children with empty URL segment
    def absorbChildrenWithEmptyUrlSegment(resource: Resource): Resource = {
      val (emptyUrlChildren, realChildren) = resource.resources.partition(_.urlSegment.isEmpty)
      val resourceWithRealChildren         = resource.copy(resources = realChildren)
      mergeResources(resourceWithRealChildren :: emptyUrlChildren)
    }

    // Group all resources at this level with the same urlSegment and urlParameter
    val groupedResources: List[List[Resource]] = resources.groupBy(_.urlSegment).values.toList

    val mergedResources: List[Resource] = groupedResources.map(mergeResources)

    val resourcesWithAbsorbedChildren = mergedResources.map(absorbChildrenWithEmptyUrlSegment)

    resourcesWithAbsorbedChildren.map { mergedAndAbsorbedResource =>
      mergedAndAbsorbedResource.copy(
        resources = unparallellizeResources(
          resources = mergedAndAbsorbedResource.resources,
          parent    = Some(mergedAndAbsorbedResource)
        )
      )
    }

  }

}
