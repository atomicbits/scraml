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

import java.util.UUID

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.TypeReference
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.parser.RamlParseException

/**
  * Created by peter on 17/12/16.
  */
case class CanonicalTypeCollector(canonicalNameGenerator: CanonicalNameGenerator) {

  implicit val cNGenerator: CanonicalNameGenerator = canonicalNameGenerator

  def collect(raml: Raml): (Raml, CanonicalLookup) = {

    val canonicalLookupHelper = CanonicalLookupHelper()

    val canonicalLookupWithIndexedParsedTypes = indexParsedTypes(raml, canonicalLookupHelper)
    val (ramlWithCanonicalReferences, canonicalLookupHelperWithCanonicalTypes) =
      collectCanonicals(raml, canonicalLookupWithIndexedParsedTypes)

    val canonicalLookup = CanonicalLookup(canonicalLookupHelperWithCanonicalTypes.lookupTable)

    (ramlWithCanonicalReferences, canonicalLookup)
  }

  def indexParsedTypes(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {
    // First, index all the parsed types on their Id so that we can perform forward lookups when creating the canonical types.
    val canonicalLookupHelperWithJsonSchemas  = raml.types.typeReferences.foldLeft(canonicalLookupHelper)(indexParsedTypes)
    val canonicalLookupHelperWithResoureTypes = raml.resources.foldLeft(canonicalLookupHelperWithJsonSchemas)(indexResourceParsedTypes)

    canonicalLookupHelperWithResoureTypes
  }

  /**
    * Collect all types in the canonical lookup helper and add the canonical references to the Raml object.
    */
  def collectCanonicals(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): (Raml, CanonicalLookupHelper) = {

    // Now, we can create all canonical types and fill in all the TypeReferences in the RAML model's TypeRepresentation instances,
    // which are located in the BodyContent and ParsedParameter objects.
    val canonicalLookupWithCanonicals = transformParsedTypeIndex(canonicalLookupHelper)

    val ramlUpdated = transformResourceParsedTypes(raml, canonicalLookupWithCanonicals)

    (ramlUpdated, canonicalLookupWithCanonicals)
  }

  private def indexParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, idWithParsedType: (Id, ParsedType)): CanonicalLookupHelper = {
    val (id, parsedType) = idWithParsedType

    val finalCanonicalLookupHelper: CanonicalLookupHelper =
      (parsedType.id, id) match {
        case (absoluteId: AbsoluteId, nativeId: NativeId) =>
          val expandedParsedType   = expandRelativeToAbsoluteIds(parsedType)
          val collectedParsedTypes = collectJsonSchemaParsedTypes(expandedParsedType)
          val updatedCLH           = canonicalLookupHelper.addJsonSchemaNativeToAbsoluteIdTranslation(nativeId, absoluteId)

          collectedParsedTypes.foldLeft(updatedCLH) { (canonicalLH, parsedType) =>
            canonicalLH.addParsedTypeIndex(parsedType.id, parsedType)
          }
        case (nativeId: NativeId, _) =>
          canonicalLookupHelper.addParsedTypeIndex(nativeId, parsedType)
        case (_, NoId) =>
          val expandedParsedType   = expandRelativeToAbsoluteIds(parsedType)
          val collectedParsedTypes = collectJsonSchemaParsedTypes(expandedParsedType)

          collectedParsedTypes.foldLeft(canonicalLookupHelper) { (canonicalLH, parsedType) =>
            canonicalLH.addParsedTypeIndex(parsedType.id, parsedType)
          }
        case (ImplicitId, someNativeId) =>
          canonicalLookupHelper.addParsedTypeIndex(someNativeId, parsedType)
        case (unexpected, _) =>
          sys.error(s"Unexpected id in the types definition: $unexpected")
      }

    finalCanonicalLookupHelper
  }

  private def indexResourceParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, resource: Resource): CanonicalLookupHelper = {
    // Types are located in the request and response BodyContent and in the ParsedParameter instances. For now we don't expect any
    // complex types in the ParsedParameter instances, so we skip those.

    // ToDo: also handle complex types in the ParsedParameter objects.

    def indexBodyParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, body: Body): CanonicalLookupHelper = {
      val bodyContentList: List[BodyContent]               = body.contentMap.values.toList
      val bodyParsedTypes: List[ParsedType]                = bodyContentList.flatMap(_.bodyType).map(_.parsed)
      val generatedNativeIds: List[Id]                     = bodyParsedTypes.map(x => NoId)
      val nativeIdsWithParsedTypes: List[(Id, ParsedType)] = generatedNativeIds.zip(bodyParsedTypes)
      nativeIdsWithParsedTypes.foldLeft(canonicalLookupHelper)(indexParsedTypes)
    }

    def indexActionParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, action: Action): CanonicalLookupHelper = {
      val canonicalLHWithBody = indexBodyParsedTypes(canonicalLookupHelper, action.body)

      val responseBodies: List[Body] = action.responses.responseMap.values.toList.map(_.body)
      responseBodies.foldLeft(canonicalLHWithBody)(indexBodyParsedTypes)
    }

    val canonicalLookupHelperWithActionTypes = resource.actions.foldLeft(canonicalLookupHelper)(indexActionParsedTypes)

    resource.resources.foldLeft(canonicalLookupHelperWithActionTypes)(indexResourceParsedTypes)
  }

  private def transformParsedTypeIndex(canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {

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

  private def transformResourceParsedTypes(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): Raml = {

    def transformTypeRepresentation(typeRepresentation: TypeRepresentation): TypeRepresentation = {
      val expandedParsedType = expandRelativeToAbsoluteIds(typeRepresentation.parsed)
      val (genericReferrable, updatedCanonicalLH) =
        ParsedToCanonicalTypeTransformer.transform(expandedParsedType, canonicalLookupHelper, None)
      // We can ignore the updatedCanonicalLH here because we know this parsed type is already registered by transformParsedTypeIndex
      genericReferrable match {
        case typeReference: TypeReference => typeRepresentation.copy(canonical = Some(typeReference))
        case other =>
          sys.error(s"We did not expect a generic type reference directly in the RAML model: ${typeRepresentation.parsed}.")
      }
    }

    def transformParsedParameter(parsedParameter: ParsedParameter): ParsedParameter = {
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
      val transformedActions      = resource.actions.map(transformAction)
      val transformedSubResources = resource.resources.map(transformResource)
      resource.copy(actions = transformedActions, resources = transformedSubResources)
    }

    val updatedResources = raml.resources.map(transformResource)
    raml.copy(resources = updatedResources)
  }

  /**
    * Collect all json-schema types
    * @param ttype
    * @return
    */
  def collectJsonSchemaParsedTypes(ttype: ParsedType): List[ParsedType] = {

    def collectFromProperties(properties: ParsedProperties, collected: List[ParsedType]): List[ParsedType] = {
      properties.values.foldLeft(collected) { (collection, property) =>
        collect(property.propertyType.parsed, collection, onlyObjectsAndEnums = true)
      }
    }

    def collectFromFragments(fragment: Fragments, collected: List[ParsedType]): List[ParsedType] = {
      val fragmentTypes: List[ParsedType] = fragment.fragments.values
      fragmentTypes.foldLeft(collected) { (collection, pType) =>
        collect(pType, collection)
      }
    }

    def collect(theType: ParsedType, collected: List[ParsedType] = List.empty, onlyObjectsAndEnums: Boolean = false): List[ParsedType] = {

      theType match {
        case objectType: ParsedObject =>
          val collectionWithProperties = collectFromProperties(objectType.properties, collected)
          val collectionWithFragments  = collectFromFragments(objectType.fragments, collectionWithProperties)
          objectType :: collectionWithFragments
        case enumType: ParsedEnum => enumType :: collected
        case fragment: Fragments =>
          collectFromFragments(fragment.fragments, collected)
        case arrayType: ParsedArray =>
          val collectionWithArrayType = collect(arrayType.items, collected, onlyObjectsAndEnums)
          val collectionWithFragments = collectFromFragments(arrayType.fragments, collectionWithArrayType)
          if (onlyObjectsAndEnums) collectionWithFragments else arrayType :: collectionWithFragments
        case typeReference: ParsedTypeReference =>
          val collectionWithFragments = collectFromFragments(typeReference.fragments, collected)
          if (onlyObjectsAndEnums) collectionWithFragments else typeReference :: collectionWithFragments
        case other =>
          if (onlyObjectsAndEnums) collected else other :: collected
      }
    }

    ttype.id match {
      case rootId: RootId => collect(ttype)
      case other          => List(ttype)
    }

  }

  /**
    * Expand all relative ids to absolute ids and register them in the type lookup and also expand all $ref pointers.
    *
    * @param ttype
    * @return
    */
  def expandRelativeToAbsoluteIds(ttype: ParsedType): ParsedType = {

    /**
      * Expand the ids in a schema based on the nearest root id of the enclosing schemas.
      *
      * @param ttype         the schema whose ids need expanding
      * @param root          the nearest (original) root id that was found in the enclosing schemas
      * @param expandingRoot the root that we're expanding (creating) based on the seed (the nearest original root id)
      * @param path          the fragment path we're on
      * @return a copy of the original schema in which all ids are replaced by root ids
      */
    def expandWithRootAndPath(ttype: ParsedType, root: RootId, expandingRoot: RootId, path: List[String] = List.empty): ParsedType = {

      val currentRoot =
        ttype.id match {
          case absId: RootId => absId
          case _             => root
        }

      val expandedId = root.toAbsolute(ttype.id, path)

      def expandProperty(property: ParsedProperty): ParsedProperty = {
        // Treat the property as a fragment to expand it.
        val fragment             = (property.name, property.propertyType.parsed)
        val (name, expandedType) = expandFragment(fragment)
        property.copy(propertyType = TypeRepresentation(expandedType))
      }

      def expandFragment(fragmentPath: (String, ParsedType)): (String, ParsedType) = {
        val (pathPart, subSchema) = fragmentPath
        val updatedSubSchema      = expandWithRootAndPath(subSchema, currentRoot, expandedId.rootPart, path :+ pathPart)
        (pathPart, updatedSubSchema)
      }

      val parsedTypeWithUpdatedFragments: ParsedType =
        ttype match {
          case objectType: ParsedObject =>
            objectType.copy(
              fragments  = objectType.fragments.map(expandFragment),
              properties = objectType.properties.map(expandProperty),
              selection = objectType.selection
                .map(select => select.map(schema => expandWithRootAndPath(schema, currentRoot, expandingRoot, path)))
            )
          case fragment: Fragments => fragment.map(expandFragment)
          case arrayType: ParsedArray =>
            val expandedPath = expandWithRootAndPath(arrayType.items, currentRoot, expandingRoot, path :+ "items")
            arrayType.copy(
              items     = expandedPath,
              fragments = arrayType.fragments.map(expandFragment)
            )
          case typeReference: ParsedTypeReference =>
            typeReference.copy(
              refersTo  = currentRoot.toAbsolute(typeReference.refersTo, path),
              fragments = typeReference.fragments.map(expandFragment)
            )
          case _ => ttype
        }

      parsedTypeWithUpdatedFragments.updated(expandedId)
    }

    ttype.id match {
      case rootId: RootId     => expandWithRootAndPath(ttype, rootId, rootId)
      case nativeId: NativeId => ttype
      case ImplicitId         =>
        // We assume we hit an inline schema without an id, so we may just invent a random unique one since it will never be referenced.
        val canonicalName = canonicalNameGenerator.generate(ImplicitId)
        val rootId        = RootId.fromCanonical(canonicalName)
        expandWithRootAndPath(ttype.updated(rootId), rootId, rootId)
      case _ => throw RamlParseException("We cannot expand the ids in a schema that has no absolute root id.")
    }

  }

  /**
    * Index all json-schema definitions on their absolute id so that we can lookup references later on.
    */
//  private def indexJsonSchemaReferences(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {
  // First index all json-schema references in the raml.types type map.

  // Next, index all json-schema references inside the resources (in the request and response bodies).

//  }

}
