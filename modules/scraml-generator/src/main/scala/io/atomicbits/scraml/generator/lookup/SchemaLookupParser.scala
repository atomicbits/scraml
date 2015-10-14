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

package io.atomicbits.scraml.generator.lookup

import io.atomicbits.scraml.generator.model.Language
import io.atomicbits.scraml.jsonschemaparser.model._
import io.atomicbits.scraml.jsonschemaparser.{AbsoluteId, JsonSchemaParseException, RootId}

import scala.annotation.tailrec


object SchemaLookupParser {


  def parse(schemas: Map[String, Schema])(implicit lang: Language): SchemaLookup = {
    schemas
      .mapValues(expandRelativeToAbsoluteIds) // we are now sure to have only AbsoluteId references as ids
      .foldLeft(SchemaLookup())(updateLookupTableAndObjectMap)
      .map(updateObjectHierarchy)
      .map(updateTypeDiscriminatorFields)
      .map(SchemaClassRepAssembler.deduceClassReps)
  }


  /**
   * Expand all relative ids to absolute ids and register them in the schema lookup and also expand all $ref pointers.
   *
   * @param schema
   * @return
   */
  def expandRelativeToAbsoluteIds(schema: Schema): Schema = {

    /**
     * Expand the ids in a schema based on the nearest root id of the enclosing schemas.
     *
     * @param schema the schema whose ids need expanding
     * @param root the nearest (original) root id that was found in the enclosing schemas
     * @param expandingRoot the root that we're expanding (creating) based on the seed (the nearest original root id)
     * @param path the fragment path we're on
     * @return a copy of the original schema in which all ids are replaced by root ids
     */
    def expandWithRootAndPath(schema: Schema, root: RootId, expandingRoot: RootId, path: List[String] = List.empty): Schema = {

      val currentRoot =
        schema.id match {
          case absId: RootId => absId
          case _             => root
        }

      val expandedId = root.toAbsolute(schema.id, path)

      def expandFragment(fragmentPath: (String, Schema)): (String, Schema) = {
        val (pathPart, subSchema) = fragmentPath
        val updatedSubSchema = expandWithRootAndPath(subSchema, currentRoot, expandedId.rootPart, path :+ pathPart)
        (pathPart, updatedSubSchema)
      }

      val schemaWithUpdatedFragments =
        schema match {
          case objEl: ObjectEl      =>
            objEl.copy(
              fragments = objEl.fragments.map(expandFragment),
              properties = objEl.properties.map(expandFragment),
              selection = objEl.selection.map(select => select.map(schema => expandWithRootAndPath(schema, currentRoot, expandingRoot, path)))
            )
          case frag: Fragment       => frag.copy(fragments = frag.fragments.map(expandFragment))
          case arr: ArrayEl         =>
            val (_, expanded) = expandFragment(("items", arr.items))
            arr.copy(
              items = expanded,
              fragments = arr.fragments.map(expandFragment)
            )
          case ref: SchemaReference =>
            ref.copy(
              refersTo = currentRoot.toAbsolute(ref.refersTo, path),
              fragments = ref.fragments.map(expandFragment)
            )
          case _                    => schema
        }

      //      val schemaWithUpdatedProperties =
      //        schemaWithUpdatedFragments match {
      //          case objEl: ObjectEl => objEl.copy(properties = objEl.properties.map(expandFragment))
      //          case _               => schemaWithUpdatedFragments
      //        }

      schemaWithUpdatedFragments.updated(expandedId)
    }

    schema.id match {
      case rootId: RootId => expandWithRootAndPath(schema, rootId, rootId)
      case _              => throw JsonSchemaParseException("We cannot expand the ids in a schema that has no absolute root id.")
    }

  }


  /**
   *
   * @param lookup The schema lookup
   * @param linkedSchema A tuple containing a field name and the schema the field refers to. Nothing is done with the
   *                     field name, it is there to make folding easier on schema fragments and object properties.
   * @return The schema lookup with added object references.
   */
  def updateLookupTableAndObjectMap(lookup: SchemaLookup, linkedSchema: (String, Schema)): SchemaLookup = {


    def updateLookupAndObjectMapInternal(lookup: SchemaLookup, schemaFragment: (String, Schema)): SchemaLookup = {

      val (path, schema) = schemaFragment

      val updatedSchemaLookup =
        schema.id match {
          case rootId: RootId =>
            lookup.copy(lookupTable = lookup.lookupTable + (rootId -> schema))
          case _              => lookup
        }

      val absoluteId = SchemaUtil.asAbsoluteId(schema.id)

      schema match {
        case objEl: ObjectEl      =>
          val schemaLookupWithObjectFragments =
            objEl.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
          val schemaLookupWithObjectProperties =
            objEl.properties.foldLeft(schemaLookupWithObjectFragments)(updateLookupAndObjectMapInternal)
          val schemaLookupWithSelectionObjects =
            objEl.selection.map {
              select => select.selection.map((path, _)).foldLeft(schemaLookupWithObjectProperties)(updateLookupAndObjectMapInternal)
            } getOrElse schemaLookupWithObjectProperties
          schemaLookupWithSelectionObjects
            .copy(objectMap = schemaLookupWithSelectionObjects.objectMap + (absoluteId -> ObjectElExt(objEl)))
        case arrayEl: ArrayEl     =>
          val schemaLookupWithArrayFragments =
            arrayEl.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
          updateLookupAndObjectMapInternal(schemaLookupWithArrayFragments, ("items", arrayEl.items))
        case ref: SchemaReference =>
          ref.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
        case fragment: Fragment   =>
          fragment.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
        case enumEl: EnumEl       =>
          updatedSchemaLookup.copy(enumMap = updatedSchemaLookup.enumMap + (absoluteId -> enumEl))
        case _                    => updatedSchemaLookup
      }

    }


    val (externalLink, schema) = linkedSchema

    val schemaLookupWithUpdatedExternalLinks = schema.id match {
      case id: RootId =>
        lookup.copy(externalSchemaLinks = lookup.externalSchemaLinks + (externalLink -> id))
      case _          => throw JsonSchemaParseException(s"A top-level schema must have a root id (is ${schema.id}).")
    }

    updateLookupAndObjectMapInternal(schemaLookupWithUpdatedExternalLinks, ("", schema))
  }


  /**
   * For each unprocessed object, lookup the selection references and collect al selection objects recursively and
   * fill in the parent-child relations.
   */
  def updateObjectHierarchy(schemaLookup: SchemaLookup): SchemaLookup = {

    @tailrec
    def lookupObjEl(schema: Schema): Option[ObjectElExt] = {
      schema match {
        case obj: ObjectEl        => schemaLookup.objectMap.get(SchemaUtil.asAbsoluteId(obj.id))
        case ref: SchemaReference => lookupObjEl(schemaLookup.lookupSchema(ref.refersTo))
        case _                    => None
      }
    }

    schemaLookup.objectMap.keys.foldLeft(schemaLookup) { (lookup, absId) =>

      val obj = lookup.objectMap(absId)

      val children: List[ObjectElExt] = obj.selection.map { sel =>
        sel.selection.flatMap(lookupObjEl)
      } getOrElse List.empty

      val childrenWithParent = children.map(_.copy(parent = Some(absId)))

      val updatedLookup = childrenWithParent.foldLeft(lookup) { (lkup, childObj) =>
        lkup.copy(objectMap = lkup.objectMap + (childObj.id -> childObj))
      }

      val updatedObj = obj.copy(children = childrenWithParent.map(_.id))
      val result = updatedLookup.copy(objectMap = updatedLookup.objectMap + (absId -> updatedObj))
      result
    }

  }


  /**
   * Check if there is a type field present in each leaf-object that is an EnumEl with one element and fill in the
   * typeDiscriminatorValue field in each of them.
   */
  def updateTypeDiscriminatorFields(schemaLookup: SchemaLookup): SchemaLookup = {

    schemaLookup.objectMap.foldLeft(schemaLookup) { (lookup, objPair) =>
      val (absId, obj) = objPair
      if (obj.hasParent && !obj.hasChildren) {
        val typeDiscriminator = obj.topLevelParent(schemaLookup).flatMap(_.typeDiscriminator).getOrElse("type")
        val discriminator = obj.properties.get(typeDiscriminator).flatMap(ObjectEl.schemaToDiscriminatorValue)

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
