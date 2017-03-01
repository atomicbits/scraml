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

import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParseException

/**
  * Created by peter on 14/02/17.
  */
case class ParsedTypeIndexer(canonicalNameGenerator: CanonicalNameGenerator) {

  def indexParsedTypes(raml: Raml, canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {
    // First, index all the parsed types on their Id so that we can perform forward lookups when creating the canonical types.
    val canonicalLookupHelperWithJsonSchemas  = raml.types.typeReferences.foldLeft(canonicalLookupHelper)(indexParsedTypesInt)
    val canonicalLookupHelperWithResoureTypes = raml.resources.foldLeft(canonicalLookupHelperWithJsonSchemas)(indexResourceParsedTypes)

    canonicalLookupHelperWithResoureTypes
  }

  private def indexParsedTypesInt(canonicalLookupHelper: CanonicalLookupHelper,
                                  idWithParsedType: (Id, ParsedType)): CanonicalLookupHelper = {

    val (id, parsedType)   = idWithParsedType
    val expandedParsedType = expandRelativeToAbsoluteIds(parsedType)

    (expandedParsedType.id, id) match {
      case (absoluteId: AbsoluteId, nativeId: NativeId) =>
        val lookupWithCollectedParsedTypes = collectJsonSchemaParsedTypes(expandedParsedType, canonicalLookupHelper)
        val updatedCLH                     = lookupWithCollectedParsedTypes.addJsonSchemaNativeToAbsoluteIdTranslation(nativeId, absoluteId)
        updatedCLH
      case (nativeId: NativeId, _) =>
        canonicalLookupHelper.addParsedTypeIndex(nativeId, expandedParsedType)
      case (_, NoId) => // ToDo: see if we can get rid of this 'NoId' pseudo id
        val lookupWithCollectedParsedTypes = collectJsonSchemaParsedTypes(expandedParsedType, canonicalLookupHelper)
        lookupWithCollectedParsedTypes
      case (ImplicitId, someNativeId) =>
        canonicalLookupHelper.addParsedTypeIndex(someNativeId, expandedParsedType)
      case (unexpected, _) =>
        sys.error(s"Unexpected id in the types definition: $unexpected")
    }
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
      nativeIdsWithParsedTypes.foldLeft(canonicalLookupHelper)(indexParsedTypesInt)
    }

    def indexActionParsedTypes(canonicalLookupHelper: CanonicalLookupHelper, action: Action): CanonicalLookupHelper = {
      val canonicalLHWithBody = indexBodyParsedTypes(canonicalLookupHelper, action.body)

      val responseBodies: List[Body] = action.responses.responseMap.values.toList.map(_.body)
      responseBodies.foldLeft(canonicalLHWithBody)(indexBodyParsedTypes)
    }

    val canonicalLookupHelperWithActionTypes = resource.actions.foldLeft(canonicalLookupHelper)(indexActionParsedTypes)

    resource.resources.foldLeft(canonicalLookupHelperWithActionTypes)(indexResourceParsedTypes)
  }

  /**
    * Collect all json-schema types
    */
  def collectJsonSchemaParsedTypes(ttype: ParsedType, canonicalLookupHelper: CanonicalLookupHelper): CanonicalLookupHelper = {

    def collectFromProperties(properties: ParsedProperties,
                              canonicalLH: CanonicalLookupHelper,
                              lookupOnly: Boolean,
                              typeDiscriminator: Option[String]): CanonicalLookupHelper = {
      properties.values.foldLeft(canonicalLH) { (canLH, property) =>
        typeDiscriminator
          .collect {
            // We don't register the type behind the type discriminator property, because we will handle it separately
            // when creating canonical types.
            case typeDisc if property.name == typeDisc => canonicalLH
          }
          .getOrElse {
            collect(
              theType             = property.propertyType.parsed,
              canonicalLH         = canLH,
              onlyObjectsAndEnums = true, // We only include objects and enums here because ... (?) {needs documentation!}
              lookupOnly          = lookupOnly
            ) // Mind that the typeDiscriminator value is NOT propagated here!
          }
      }
    }

    def collectFromFragments(fragment: Fragments, canonicalLH: CanonicalLookupHelper, lookupOnly: Boolean): CanonicalLookupHelper = {
      val fragmentTypes: List[ParsedType] = fragment.fragments.values
      fragmentTypes.foldLeft(canonicalLH) { (canLH, pType) =>
        collect(theType = pType, canonicalLH = canLH, lookupOnly = lookupOnly)
      }
    }

    def collectFromSelection(selection: Selection,
                             canonicalLH: CanonicalLookupHelper,
                             typeDiscriminator: Option[String]): CanonicalLookupHelper = {
      val selectionTypes: List[ParsedType] = selection.selection
      selectionTypes.foldLeft(canonicalLH) { (canLH, pType) =>
        // Selection types will not be added for generation, but for lookup only, they will be generated through their parent definition.
        collect(theType = pType, canonicalLH = canLH, lookupOnly = true, typeDiscriminator = typeDiscriminator)
        // Mind that the typeDiscriminator value is MUST be propagated here!
      }
    }

    def collect(theType: ParsedType,
                canonicalLH: CanonicalLookupHelper,
                onlyObjectsAndEnums: Boolean      = false,
                lookupOnly: Boolean               = false,
                typeDiscriminator: Option[String] = None): CanonicalLookupHelper = {

      theType match {
        case objectType: ParsedObject =>
          val actualTypeDiscriminator = List(typeDiscriminator, objectType.typeDiscriminator).flatten.headOption
          // Mind that the typeDiscriminator value is only propagated with the properties and the selection!
          val lookupWithProperties = collectFromProperties(objectType.properties, canonicalLH, lookupOnly, actualTypeDiscriminator)
          val lookupWithFragments  = collectFromFragments(objectType.fragments, lookupWithProperties, lookupOnly)
          val lookupWithSelection =
            objectType.selection
              .map(collectFromSelection(_, lookupWithFragments, actualTypeDiscriminator))
              .getOrElse(lookupWithFragments)
          lookupWithSelection.addParsedTypeIndex(id = objectType.id, parsedType = objectType, lookupOnly = lookupOnly)
        case enumType: ParsedEnum => canonicalLH.addParsedTypeIndex(enumType.id, enumType)
        case fragment: Fragments  => collectFromFragments(fragment.fragments, canonicalLH, lookupOnly)
        case arrayType: ParsedArray =>
          val lookupWithArrayType = collect(arrayType.items, canonicalLH, onlyObjectsAndEnums, lookupOnly)
          val lookupWithFragments = collectFromFragments(arrayType.fragments, lookupWithArrayType, lookupOnly)
          if (onlyObjectsAndEnums) lookupWithFragments
          else lookupWithFragments.addParsedTypeIndex(id = arrayType.id, parsedType = arrayType, lookupOnly = lookupOnly)
        case typeReference: ParsedTypeReference =>
          val lookupWithFragments = collectFromFragments(typeReference.fragments, canonicalLH, lookupOnly)
          if (onlyObjectsAndEnums) lookupWithFragments
          else lookupWithFragments.addParsedTypeIndex(id = typeReference.id, parsedType = typeReference, lookupOnly = lookupOnly)
        case other =>
          if (onlyObjectsAndEnums) canonicalLH
          else canonicalLH.addParsedTypeIndex(id = other.id, parsedType = other, lookupOnly = lookupOnly)
      }
    }

    ttype.id match {
      case rootId: RootId => collect(ttype, canonicalLookupHelper)
      case relId: RelativeId =>
        val rootId: RootId = canonicalNameGenerator.toRootId(relId)
        collect(ttype.updated(rootId), canonicalLookupHelper)
      case other => canonicalLookupHelper
    }

  }

  /**
    * Expand all relative ids to absolute ids and also expand all $ref pointers.
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
          case rootId: RootId => rootId
          case _              => root
        }

      val expandedId = root.toAbsoluteId(ttype.id, path)

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
              refersTo  = currentRoot.toAbsoluteId(typeReference.refersTo, path),
              fragments = typeReference.fragments.map(expandFragment)
            )
          case _ => ttype
        }

      parsedTypeWithUpdatedFragments.updated(expandedId)
    }

    if (hasNativeIds(ttype)) {
      ttype
    } else {
      ttype.id match {
        case rootId: RootId => expandWithRootAndPath(ttype, rootId, rootId)
        case relativeId: RelativeId =>
          val anonymousRootId = canonicalNameGenerator.toRootId(NativeId("anonymous"))
          expandWithRootAndPath(ttype, anonymousRootId, anonymousRootId)
        case ImplicitId =>
          // We assume we hit an inline schema without an id, so we may just invent a random unique one since it will never be referenced.
          val canonicalName = canonicalNameGenerator.generate(ImplicitId)
          val rootId        = RootId.fromCanonical(canonicalName)
          expandWithRootAndPath(ttype.updated(rootId), rootId, rootId)
        case _ => throw RamlParseException("We cannot expand the ids in a schema that has no absolute root id.")
      }
    }

  }

  private def hasNativeIds(ttype: ParsedType): Boolean = {

    def isNativeId(id: Id): Boolean =
      id match {
        case nativeId: NativeId => true
        case _                  => false
      }

    def hasDeepNativeIds(deepType: ParsedType): Boolean =
      deepType match {
        case objectType: ParsedObject =>
          val hasNativeIdList = objectType.properties.valueMap.values.map(property => hasNativeIds(property.propertyType.parsed)).toList
          hasNativeIdList match {
            case Nil                => false
            case i1 :: Nil          => i1
            case atLeastTwoElements => atLeastTwoElements.reduce(_ || _)
          }
        case arrayType: ParsedArray             => hasNativeIds(arrayType.items)
        case typeReference: ParsedTypeReference => isNativeId(typeReference.refersTo)
        case _                                  => false
      }

    isNativeId(ttype.id) || hasDeepNativeIds(ttype)
  }

}
