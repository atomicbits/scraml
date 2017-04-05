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

package io.atomicbits.scraml.ramlparser.lookup

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.TypeReference
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 17/12/16.
  *
  *
  */
case class CanonicalTypeCollector(canonicalNameGenerator: CanonicalNameGenerator) {

  implicit val cNGenerator: CanonicalNameGenerator = canonicalNameGenerator

  val indexer = ParsedTypeIndexer(canonicalNameGenerator)

  def collect(raml: Raml): (Raml, CanonicalLookup) = {

    val canonicalLookupHelper = CanonicalLookupHelper()

    val canonicalLookupWithIndexedParsedTypes = indexer.indexParsedTypes(raml, canonicalLookupHelper)
    val (ramlWithCanonicalReferences, canonicalLookupHelperWithCanonicalTypes) =
      collectCanonicals(raml, canonicalLookupWithIndexedParsedTypes)

    val canonicalLookup = CanonicalLookup(canonicalLookupHelperWithCanonicalTypes.lookupTable)

    (ramlWithCanonicalReferences, canonicalLookup)
  }

  /**
    * Collect all types in the canonical lookup helper and add the canonical references to the Raml object.
    */
  def collectCanonicals(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): (Raml, CanonicalLookupHelper) = {

    // Now, we can create all canonical types and fill in all the TypeReferences in the RAML model's TypeRepresentation instances,
    // which are located in the BodyContent and ParsedParameter objects.
    val canonicalLookupWithCanonicals = transformParsedTypeIndexToCanonicalTypes(canonicalLookupHelper)

    val ramlUpdated = transformResourceParsedTypesToCanonicalTypes(raml, canonicalLookupWithCanonicals)

    (ramlUpdated, canonicalLookupWithCanonicals)
  }

  private def transformParsedTypeIndexToCanonicalTypes(canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {

    canonicalLookupHelper.parsedTypeIndex.foldLeft(canonicalLookupHelper) { (canonicalLH, idWithParsedType) =>
      val (id, parsedType) = idWithParsedType
      parsedType.id match {
        case ImplicitId =>
          val generatedCanonicalName = canonicalNameGenerator.generate(id)
          val (canonicalType, updatedCanonicalLH) =
            ParsedToCanonicalTypeTransformer.transform(parsedType, canonicalLH, Some(generatedCanonicalName))
          updatedCanonicalLH
        case otherId =>
          val (canonicalType, updatedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(parsedType, canonicalLH, None)
          updatedCanonicalLH
      }
    }

  }

  private def transformResourceParsedTypesToCanonicalTypes(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): Raml = {

    def transformTypeRepresentation(typeRepresentation: TypeRepresentation): TypeRepresentation = {
      val expandedParsedType = indexer.expandRelativeToAbsoluteIds(typeRepresentation.parsed)
      val (genericReferrable, updatedCanonicalLH) =
        ParsedToCanonicalTypeTransformer.transform(expandedParsedType, canonicalLookupHelper, None)
      // We can ignore the updatedCanonicalLH here because we know this parsed type is already registered by transformParsedTypeIndex
      genericReferrable match {
        case typeReference: TypeReference => typeRepresentation.copy(canonical = Some(typeReference))
        case other =>
          sys.error(s"We did not expect a generic type reference directly in the RAML model: ${typeRepresentation.parsed}.")
      }
    }

    def transformParsedParameter(parsedParameter: Parameter): Parameter = {
      val updatedParameterType = transformTypeRepresentation(parsedParameter.parameterType)
      parsedParameter.copy(parameterType = updatedParameterType)
    }

    def transformBodyContent(bodyContent: BodyContent): BodyContent = {
      val updatedFormParameters = bodyContent.formParameters.mapValues(transformParsedParameter)
      val updatedBodyType       = bodyContent.bodyType.map(transformTypeRepresentation)
      bodyContent.copy(formParameters = updatedFormParameters, bodyType = updatedBodyType)
    }

    def transformBody(body: Body): Body = {
      val updatedContentMap = body.contentMap.mapValues(transformBodyContent)
      body.copy(contentMap = updatedContentMap)
    }

    def transformAction(action: Action): Action = {

      val updatedHeaders = action.headers.mapValues(transformParsedParameter)

      val updatedQueryParameters = action.queryParameters.mapValues(transformParsedParameter)

      val updatedBody = transformBody(action.body)

      val updatedResponseMap = action.responses.responseMap.mapValues { response =>
        val updatedResponseBody = transformBody(response.body)
        response.copy(body = updatedResponseBody)
      }
      val updatedResponses = action.responses.copy(responseMap = updatedResponseMap)

      action.copy(headers = updatedHeaders, queryParameters = updatedQueryParameters, body = updatedBody, responses = updatedResponses)
    }

    def transformResource(resource: Resource): Resource = {
      val transformedUrlParameter = resource.urlParameter.map(transformParsedParameter)
      val transformedActions      = resource.actions.map(transformAction)
      val transformedSubResources = resource.resources.map(transformResource)
      resource.copy(urlParameter = transformedUrlParameter, actions = transformedActions, resources = transformedSubResources)
    }

    val updatedResources = raml.resources.map(transformResource)
    raml.copy(resources = updatedResources)
  }

}
