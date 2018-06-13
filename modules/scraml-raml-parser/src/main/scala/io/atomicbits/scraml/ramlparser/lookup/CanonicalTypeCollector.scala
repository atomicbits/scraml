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

package io.atomicbits.scraml.ramlparser.lookup

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, TypeReference }
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import org.slf4j.{ Logger, LoggerFactory }

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

    def injectCanonicalTypeRepresentation(typeRepresentation: TypeRepresentation,
                                          nameSuggestion: Option[CanonicalName] = None): TypeRepresentation = {
      val expandedParsedType = indexer.expandRelativeToAbsoluteIds(typeRepresentation.parsed)
      val (genericReferrable, updatedCanonicalLH) =
        ParsedToCanonicalTypeTransformer.transform(expandedParsedType, canonicalLookupHelper, nameSuggestion)
      // We can ignore the updatedCanonicalLH here because we know this parsed type is already registered by transformParsedTypeIndexToCanonicalTypes

      genericReferrable match {
        case typeReference: TypeReference => typeRepresentation.copy(canonical = Some(typeReference))
        case other =>
          sys.error(s"We did not expect a generic type reference directly in the RAML model: ${typeRepresentation.parsed}.")
      }
    }

    def transformQueryString(queryString: QueryString): QueryString = {
      val updatedQueryStringType = injectCanonicalTypeRepresentation(queryString.queryStringType)
      queryString.copy(queryStringType = updatedQueryStringType)
    }

    def transformParsedParameter(parsedParameter: Parameter): Parameter = {
      val canonicalNameSuggestion = canonicalNameGenerator.generate(NativeId(parsedParameter.name))
      val updatedParameterType    = injectCanonicalTypeRepresentation(parsedParameter.parameterType, Some(canonicalNameSuggestion))
      parsedParameter.copy(parameterType = updatedParameterType)
    }

    def transformBodyContent(bodyContent: BodyContent): BodyContent = {
      val updatedFormParameters = bodyContent.formParameters.mapValues(transformParsedParameter)
      val updatedBodyType       = bodyContent.bodyType.map(injectCanonicalTypeRepresentation(_))
      bodyContent.copy(formParameters = updatedFormParameters, bodyType = updatedBodyType)
    }

    def transformBody(body: Body): Body = {
      val updatedContentMap = body.contentMap.mapValues(transformBodyContent)
      body.copy(contentMap = updatedContentMap)
    }

    def transformAction(action: Action): Action = {

      val updatedHeaders = action.headers.mapValues(transformParsedParameter)

      val updatedQueryParameters = action.queryParameters.mapValues(transformParsedParameter)

      val updatedQueryString = action.queryString.map(transformQueryString)

      val updatedBody = transformBody(action.body)

      val updatedResponseMap = action.responses.responseMap.mapValues { response =>
        val updatedResponseBody = transformBody(response.body)
        response.copy(body = updatedResponseBody)
      }
      val updatedResponses = action.responses.copy(responseMap = updatedResponseMap)

      action.copy(headers         = updatedHeaders,
                  queryParameters = updatedQueryParameters,
                  queryString     = updatedQueryString,
                  body            = updatedBody,
                  responses       = updatedResponses)
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
