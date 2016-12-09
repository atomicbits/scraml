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

import scala.annotation.tailrec
import scala.language.postfixOps


object TypeLookupParser {


  def parse(raml: Raml): (Raml, TypeLookupTable) = {

    val ramlExpanded: Raml = extractInlineTypes(raml)

    val typeLookupTable =
      ramlExpanded.types.typeReferences
        .map(fillInTopLevelUnrootedIds)
        .mapValues(expandRelativeToAbsoluteIds) // we are now sure to have only AbsoluteId references as ids
        .foldLeft(TypeLookupTable())(updateLookupTableAndObjectMap)
        .map(updateObjectHierarchy)
        .map(updateTypeDiscriminatorFields)

    (ramlExpanded, typeLookupTable)
  }


  /**
    * Extracts all inline types that are defined inside the resources an put them in the Types map. The extracted types are
    * replaced by a native type reference.
    */
  private def extractInlineTypes(raml: Raml): Raml = {

    var inlineNativeIdCounter = 1

    def extractFromBody(body: Body, types: Types): (Body, Types) = {

      val (updatedHeaderMap, updatedTypes) =
        body.contentMap.foldLeft((Map.empty[MediaType, BodyContent], types)) {

          case ((headerMap, ttypes), (mimeType, bodyContent)) =>

            val result: Option[(BodyContent, Types)] =
              bodyContent.bodyType.collect {
                // ToDo: refactor case statements below, it's too cumbersome and there is too much repetition
                case typeReference: ParsedTypeReference =>
                  typeReference.id match {
                    case nativeId: NativeId =>
                      // No updated needed, it's already a native reference type.
                      (bodyContent, ttypes)
                    case otherId            =>
                      val nativeId = NativeId(s"inline$inlineNativeIdCounter")
                      inlineNativeIdCounter += 1
                      val nativeTypeReference = ParsedTypeReference(nativeId)
                      val updatedBodyContent = bodyContent.copy(bodyType = Some(nativeTypeReference))
                      (updatedBodyContent, ttypes + (nativeId -> typeReference))
                  }

                case otherType =>
                  val nativeId = NativeId(s"inline$inlineNativeIdCounter")
                  inlineNativeIdCounter += 1
                  val nativeTypeReference = ParsedTypeReference(nativeId)
                  val updatedBodyContent = bodyContent.copy(bodyType = Some(nativeTypeReference))
                  (updatedBodyContent, ttypes + (nativeId -> otherType))
              }

            result.map {
              case (updatedBodyContent, updatedTtypes) => (headerMap + (mimeType -> updatedBodyContent), updatedTtypes)
            } getOrElse(headerMap + (mimeType -> bodyContent), ttypes)
        }

      val updatedBody = body.copy(contentMap = updatedHeaderMap)

      (updatedBody, updatedTypes)
    }

    def extractFromResponse(responses: Responses, types: Types): (Responses, Types) = {

      val (updatedResponseMap, updatedTypes) =
        responses.responseMap.foldLeft((Map.empty[StatusCode, Response], types)) {
          case ((responseMap, ttypes), (statusCode, response)) =>
            val (updatedBody, updatedTypes) = extractFromBody(response.body, ttypes)
            val updatedResponse = response.copy(body = updatedBody)
            (responseMap + (statusCode -> updatedResponse), updatedTypes)
        }

      (responses.copy(responseMap = updatedResponseMap), updatedTypes)
    }

    def extractFromResource(resource: Resource, types: Types): (Resource, Types) = {

      val (updatedActions, updatedTypes) =
        resource.actions.foldLeft((List.empty[Action], types)) {
          case ((actions, ttypes), action) =>

            val (updatedBody, updatedTypesBody) = extractFromBody(action.body, ttypes)
            val (updatedResponses, updatedTypesResponses) = extractFromResponse(action.responses, updatedTypesBody)

            val updatedAction = action.copy(body = updatedBody, responses = updatedResponses)

            (updatedAction :: actions, updatedTypesResponses)
        }

      (resource.copy(actions = updatedActions), updatedTypes)
    }

    def extract(resources: List[Resource], types: Types): (List[Resource], Types) = {

      resources.foldLeft((List.empty[Resource], types)) {
        case ((processedResources, processedTypes), resource) =>
          val (updatedParentResource, updatedTypes) = extractFromResource(resource, processedTypes)
          val (updatedChildResources, updatedTypesChildren) = extract(resource.resources, updatedTypes)
          val updatedResource = updatedParentResource.copy(resources = updatedChildResources)
          (updatedResource +: processedResources, updatedTypesChildren)
      }

    }


    val (updatedResources, updatedTypes) = extract(raml.resources, raml.types)
    raml.copy(
      resources = updatedResources,
      types = updatedTypes
    )
  }


  private def fillInTopLevelUnrootedIds(nameWithType: (NativeId, ParsedType)): (NativeId, ParsedType) = {

    val (nativeId, ttype) = nameWithType

    ttype.id match {
      case ImplicitId => (nativeId, ttype.updated(nativeId))
      case _          => (nativeId, ttype)
    }

  }


  /**
    * Expand all relative ids to absolute ids and register them in the type lookup and also expand all $ref pointers.
    *
    * @param ttype
    * @return
    */
  private def expandRelativeToAbsoluteIds(ttype: ParsedType): ParsedType = {

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


      def expandProperty(property: Property): Property = {
        // Treat the property as a fragment to expand it.
        val fragment = (property.name, property.propertyType)
        val (name, expandedType) = expandFragment(fragment)
        property.copy(propertyType = expandedType)
      }

      def expandFragment(fragmentPath: (String, ParsedType)): (String, ParsedType) = {
        val (pathPart, subSchema) = fragmentPath
        val updatedSubSchema = expandWithRootAndPath(subSchema, currentRoot, expandedId.rootPart, path :+ pathPart)
        (pathPart, updatedSubSchema)
      }


      val schemaWithUpdatedFragments: ParsedType =
        ttype match {
          case objectType: ParsedObject           =>
            objectType.copy(
              fragments = objectType.fragments.map(expandFragment),
              properties = objectType.properties.map(expandProperty),
              selection = objectType.selection
                .map(select => select.map(schema => expandWithRootAndPath(schema, currentRoot, expandingRoot, path)))
            )
          case fragment: Fragments                => fragment.map(expandFragment)
          case arrayType: ParsedArray             =>
            val (_, expanded) = expandFragment(("items", arrayType.items))
            arrayType.copy(
              items = expanded,
              fragments = arrayType.fragments.map(expandFragment)
            )
          case typeReference: ParsedTypeReference =>
            typeReference.copy(
              refersTo = currentRoot.toAbsolute(typeReference.refersTo, path),
              fragments = typeReference.fragments.map(expandFragment)
            )
          case _                                  => ttype
        }

      schemaWithUpdatedFragments.updated(expandedId)
    }


    ttype.id match {
      case rootId: RootId     => expandWithRootAndPath(ttype, rootId, rootId)
      case nativeId: NativeId => ttype
      case ImplicitId         =>
        // We assume we hit an inline schema without an id, so we may just invent a random unique one since it will never be referenced.
        val uniqueName = UUID.randomUUID().toString
        val rootId = RootId(s"http://atomicbits.io/schema/$uniqueName.json")
        expandWithRootAndPath(ttype.updated(rootId), rootId, rootId)
      case _                  => throw RamlParseException("We cannot expand the ids in a schema that has no absolute root id.")
    }

  }


  /**
    *
    * @param lookup       The type lookup
    * @param linkedSchema A tuple containing a field name and the schema the field refers to. Nothing is done with the
    *                     field name, it is there to make folding easier on schema fragments and object properties.
    * @return The schema lookup with added object references.
    */
  private def updateLookupTableAndObjectMap(lookup: TypeLookupTable, linkedSchema: (NativeId, ParsedType)): TypeLookupTable = {


    def updateLookupAndObjectMapJsonSchema(lookup: TypeLookupTable, schemaFragment: (String, ParsedType)): TypeLookupTable = {

      val (path, ttype) = schemaFragment

      val updatedSchemaLookup =
        ttype.id match {
          case rootId: RootId => lookup.copy(lookupTable = lookup.lookupTable + (rootId -> ttype))
          case _              => lookup
        }

      def uniqueId: UniqueId = TypeUtils.asUniqueId(ttype.id)

      ttype match {
        case objectType: ParsedObject           =>
          val schemaLookupWithObjectFragments =
            objectType.fragments.fragmentMap.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapJsonSchema)
          val schemaLookupWithObjectProperties =
            objectType.properties.asTypeMap.foldLeft(schemaLookupWithObjectFragments)(updateLookupAndObjectMapJsonSchema)
          val schemaLookupWithSelectionObjects =
            objectType.selection.map {
              select => select.selection.map((path, _)).foldLeft(schemaLookupWithObjectProperties)(updateLookupAndObjectMapJsonSchema)
            } getOrElse schemaLookupWithObjectProperties
          schemaLookupWithSelectionObjects
            .copy(objectMap = schemaLookupWithSelectionObjects.objectMap + (uniqueId -> objectType))
        case arrayType: ParsedArray             =>
          val schemaLookupWithArrayFragments =
            arrayType.fragments.fragmentMap.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapJsonSchema)
          updateLookupAndObjectMapJsonSchema(schemaLookupWithArrayFragments, ("items", arrayType.items))
        case typeReference: ParsedTypeReference =>
          typeReference.fragments.fragmentMap.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapJsonSchema)
        case fragment: Fragments                =>
          fragment.fragments.fragmentMap.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapJsonSchema)
        case enumType: ParsedEnum               =>
          updatedSchemaLookup.copy(enumMap = updatedSchemaLookup.enumMap + (uniqueId -> enumType))
        case _                                  => updatedSchemaLookup
      }

    }


    def updateLookupAndObjectMapNativeTypes(lookup: TypeLookupTable, ttype: ParsedType): TypeLookupTable = {

      def uniqueId: UniqueId = TypeUtils.asUniqueId(ttype.id)

      ttype match {
        case objectType: ParsedObject =>
          val schemaLookupWithObjectProperties =
            objectType.properties.types.foldLeft(lookup)(updateLookupAndObjectMapNativeTypes)
          schemaLookupWithObjectProperties
            .copy(objectMap = schemaLookupWithObjectProperties.objectMap + (uniqueId -> objectType))
        case arrayType: ParsedArray   =>
          updateLookupAndObjectMapNativeTypes(lookup, arrayType.items)
        case enumType: ParsedEnum     => lookup.copy(enumMap = lookup.enumMap + (uniqueId -> enumType))
        case _                      => lookup
      }
    }


    val (nativeId, ttype) = linkedSchema

    ttype.id match {
      case id: RootId   =>
        val schemaLookupWithUpdatedExternalLinks = lookup.copy(nativeIdMap = lookup.nativeIdMap + (nativeId -> id))
        updateLookupAndObjectMapJsonSchema(schemaLookupWithUpdatedExternalLinks, ("", ttype))
      case id: NativeId =>
        val uniqueId = TypeUtils.asUniqueId(id)
        val updatedLookup =
          lookup.copy(
            nativeIdMap = lookup.nativeIdMap + (nativeId -> uniqueId),
            lookupTable = lookup.lookupTable + (uniqueId -> ttype)
          )
        updateLookupAndObjectMapNativeTypes(updatedLookup, ttype)
      case _            => throw RamlParseException(s"A top-level schema must have a root id or a native id (is ${ttype.id}).")
    }

  }


  /**
    * For each unprocessed object, lookup the selection references and collect al selection objects recursively and
    * fill in the parent-child relations.
    */
  private def updateObjectHierarchy(lookupTable: TypeLookupTable): TypeLookupTable = {

    @tailrec
    def lookupObjEl(schema: ParsedType): Option[ParsedObject] = {
      schema match {
        case objectType: ParsedObject           => Some(objectType)
        case typeReference: ParsedTypeReference => lookupObjEl(lookupTable.lookup(typeReference.refersTo))
        case _                                  => None
      }
    }

    lookupTable.objectMap.keys.foldLeft(lookupTable) { (lookup, absId) =>

      val obj = lookup.objectMap(absId)

      val children: List[ParsedObject] = obj.selection.map { sel =>
        sel.selection.flatMap(lookupObjEl)
      } getOrElse List.empty

      val childrenWithParent = children.map(_.copy(parent = Some(absId)))

      val updatedLookup = childrenWithParent.foldLeft(lookup) { (lkup, childObj) =>
        lkup.copy(objectMap = lkup.objectMap + (TypeUtils.asUniqueId(childObj.id) -> childObj))
      }

      val updatedObj =
        obj.copy(children = childrenWithParent.map(childObj => TypeUtils.asUniqueId(childObj.id)))
      val result = updatedLookup.copy(objectMap = updatedLookup.objectMap + (absId -> updatedObj))
      result
    }

  }


  /**
    * Check if there is a type field present in each leaf-object that is an EnumEl with one element and fill in the
    * typeDiscriminatorValue field in each of them.
    */
  private def updateTypeDiscriminatorFields(lookupTable: TypeLookupTable): TypeLookupTable = {

    lookupTable.objectMap.foldLeft(lookupTable) { (lookup, objPair) =>
      val (absId, obj) = objPair
      if (obj.hasParent && !obj.hasChildren) {
        val typeDiscriminator = obj.topLevelParent(lookupTable).flatMap(_.typeDiscriminator).getOrElse("type")
        val discriminator = obj.properties.get(typeDiscriminator).map(_.propertyType).flatMap(ParsedObject.schemaToDiscriminatorValue)

        if (discriminator.isEmpty)
          println(
            s"""
               |In order to support class hierarchies, we expect objects inside the 'oneOf' part of an object to have a
               |'type' field pointing to an enum element that contains one string element that serves as a discrimitator value for
               |the type serialization.
             """.stripMargin
          )

        // We copy the typeDiscriminator to the object as well for easy access later on.
        discriminator.map { disc =>
          val updatedObj = obj.copy(typeDiscriminatorValue = Some(disc), typeDiscriminator = Some(typeDiscriminator))
          lookup.copy(objectMap = lookup.objectMap + (absId -> updatedObj))
        } getOrElse lookup

      } else {
        lookup
      }
    }

  }

}
