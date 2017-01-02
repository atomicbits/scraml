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
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.parser.RamlParseException

/**
  * Created by peter on 17/12/16.
  */
case class CanonicalTypeCollector(canonicalNameGenerator: CanonicalNameGenerator) {

  implicit val cNGenerator: CanonicalNameGenerator = canonicalNameGenerator

  def collect(raml: Raml): (Raml, CanonicalLookup) = {

    val canonicalLookupHelper = CanonicalLookupHelper()

    val (ramlWithCanonicalReferences, canonicalLookupHelperWithCanonicalTypes) = collectCanonicals(raml, canonicalLookupHelper)

    val canonicalLookup = CanonicalLookup() // ToDo

    (ramlWithCanonicalReferences, canonicalLookup)
  }

  /**
    * Collect all types in the canonical lookup helper and add the canonical references to the Raml object.
    */
  def collectCanonicals(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): (Raml, CanonicalLookupHelper) = {

    // Todo: consider to collect all json-schema fragments first throughout the types and the resources.

    val updatedCanonicalLookupHelper = processTypes(raml, canonicalLookupHelper)

    val ramlUpdated = raml // ToDo: complete RAML canonical references

    (ramlUpdated, updatedCanonicalLookupHelper)
  }

  private def processTypes(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {

    val canonicalLookupHelperWithJsonSchemas = raml.types.typeReferences.foldLeft(canonicalLookupHelper)(indexParsedTypes)

    val canonicalLookupHelperWithResoureTypes = raml.resources.foldLeft(canonicalLookupHelperWithJsonSchemas)(indexResourceParsedTypes)

    val canonicalLookupWithCanonicals = transformParsedTypeIndex(canonicalLookupHelperWithResoureTypes)

    canonicalLookupWithCanonicals
  }

  private def indexResourceParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, resource: Resource): CanonicalLookupHelper = {
    // Types are located in the request and response BodyContent and in the ParsedParameter instances. For now we don't expect any
    // complex types in the ParsedParameter instances, so we skip those.

    def indexBodyParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, body: Body): CanonicalLookupHelper = {
      val bodyContentList: List[BodyContent]                     = body.contentMap.values.toList
      val bodyParsedTypes: List[ParsedType]                      = bodyContentList.flatMap(_.bodyType).map(_.parsed)
      val generatedNativeIds: List[NativeId]                     = bodyParsedTypes.map(x => NativeId(s"Inline${UUID.randomUUID}"))
      val nativeIdsWithParsedTypes: List[(NativeId, ParsedType)] = generatedNativeIds.zip(bodyParsedTypes)
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

  private def indexParsedTypes(canonicalLookupHelper: CanonicalLookupHelper,
                               idWithParsedType: (NativeId, ParsedType)): CanonicalLookupHelper = {
    val (id, parsedType) = idWithParsedType

    val finalCanonicalLookupHelper: CanonicalLookupHelper =
      parsedType.id match {
        case absoluteId: AbsoluteId =>
          val expandedParsedType   = expandRelativeToAbsoluteIds(parsedType)
          val collectedParsedTypes = collectJsonSchemaParsedTypes(expandedParsedType)
          val updatedCLH           = canonicalLookupHelper.addJsonSchemaNativeToAbsoluteIdTranslation(id, absoluteId)

          collectedParsedTypes.foldLeft(updatedCLH) { (canonicalLH, parsedType) =>
            canonicalLH.addParsedTypeIndex(parsedType.id, parsedType)
          }
        case nativeId: NativeId =>
          canonicalLookupHelper.addParsedTypeIndex(nativeId, parsedType)
        case ImplicitId =>
          canonicalLookupHelper.addParsedTypeIndex(id, parsedType)
        case unexpected => sys.error(s"Unexpected id in the types definition: $unexpected")
      }

    finalCanonicalLookupHelper
  }

  private def transformParsedTypeIndex(canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {

    canonicalLookupHelper.parsedTypeIndex.foldLeft(canonicalLookupHelper) { (canonicalLH, idWithParsedType) =>
      val (id, parsedType) = idWithParsedType
      parsedType.id match {
        case ImplicitId =>
          val generatedCanonicalName = canonicalNameGenerator.generate(id)
          val (canonicalType, canonicalLH) =
            ParsedToCanonicalTypeTransformer.transform(parsedType, canonicalLookupHelper, Some(generatedCanonicalName))
          canonicalLH
        case otherId =>
          val (canonicalType, canonicalLH) = ParsedToCanonicalTypeTransformer.transform(parsedType, canonicalLookupHelper, None)
          canonicalLH
      }
    }

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

      val schemaWithUpdatedFragments: ParsedType =
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
            val (_, expandedFragment) = expandFragment(("items", arrayType.items))
            // val expandedPath          = expandWithRootAndPath(expandedFragment, currentRoot, expandingRoot, path :+ "items")
            arrayType.copy(
              items     = expandedFragment,
              fragments = arrayType.fragments.map(expandFragment)
            )
          case typeReference: ParsedTypeReference =>
            typeReference.copy(
              refersTo  = currentRoot.toAbsolute(typeReference.refersTo, path),
              fragments = typeReference.fragments.map(expandFragment)
            )
          case _ => ttype
        }

      schemaWithUpdatedFragments.updated(expandedId)
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
