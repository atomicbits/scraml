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

import io.atomicbits.scraml.ramlparser.lookup._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedParameters, Types }
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
                baseUriParameters: ParsedParameters,
                protocols: Option[Seq[String]],
                traits: Traits,
                types: Types,
                resources: List[Resource],
                canonicalMap: Option[OldCanonicalLookupHelper] = None) {

  /**
    * Collect all types in the type lookup table and replace all inline types with native type references that point to the
    * nativeIdMap map of the type lookup table.
    *
    * @return This changed Raml model with a type lookup table.
    */
  @Deprecated
  def collectCanonicalTypes(defaultBasePath: List[String]): Raml = {
    // uniqueIdToCanonicalName: UniqueId => CanonicalName
    val canonicalNameGenerator                                         = CanonicalNameGenerator(defaultBasePath)
    val canonicalTypeCollector                                         = OldCanonicalTypeCollector(canonicalNameGenerator)
    val (ramlExpanded, canonicalMap): (Raml, OldCanonicalLookupHelper) = canonicalTypeCollector.collect(this)
    ramlExpanded.copy(canonicalMap = Some(canonicalMap))
  }

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

    val mediaType: Try[Option[MediaType]] = {
      (ramlJson \ "mediaType").toOption.collect {
        case JsString(mType) => Success(Option(MediaType(mType)))
        case x               => Failure(RamlParseException(s"The mediaType in ${parseCtxt.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    implicit val parseContext: ParseContext = {

      val tryParseCtxt =
        for {
          newTraits <- tryTraits
          defaultMediaType <- mediaType
        } yield parseCtxt.copy(traits = newTraits, defaultMediaType = defaultMediaType)

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
      List((ramlJson \ "types").toOption, (ramlJson \ "schemas").toOption).flatten match {
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
        case JsArray(pcols) => accumulate(pcols.map(toProtocolString)).map(Some(_))
        case x =>
          Failure(RamlParseException(s"The protocols field in ${parseCtxt.sourceTrail} must be an array of string values."))
      } getOrElse Success(None)
    }

    val version: Try[Option[String]] = {
      (ramlJson \ "version").toOption.collect {
        case JsString(v) => Success(Option(v))
        case JsNumber(v) => Success(Option(v.toString()))
        case x =>
          Failure(RamlParseException(s"The version field in ${parseCtxt.sourceTrail} must be a string or a number value."))
      } getOrElse Success(None)
    }

    val baseUri: Try[Option[String]] = {
      (ramlJson \ "baseUri").toOption.collect {
        case JsString(v) => Success(Option(v))
        case x =>
          Failure(RamlParseException(s"The baseUri field in ${parseCtxt.sourceTrail} must be a string value."))
      } getOrElse Success(None)
    }

    val baseUriParameters: Try[ParsedParameters] = ParsedParameters((ramlJson \ "baseUriParameters").toOption)

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
        } toList

      TryUtils.accumulate(resourceFields).map(unparallellizeResources(_, None))
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
