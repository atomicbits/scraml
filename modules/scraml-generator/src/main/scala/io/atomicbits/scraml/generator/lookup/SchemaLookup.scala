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

import io.atomicbits.scraml.generator._
import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.jsonschemaparser.model._
import io.atomicbits.scraml.jsonschemaparser.{AbsoluteId, Id, RootId}

import scala.annotation.tailrec

/**
 * A lookup table to follow schema ids and external links to schema definitions (JsObject) and canonical names.
 *
 * @param lookupTable Maps absolute schema ids (and relative schema ids after they have been expanded to their
 *                    absolute form) to the schema definition. Mind that not all schema definitions represent
 *                    object types, they can represent any type, or even no type (usually when defining nested
 *                    schemas).
 * @param externalSchemaLinks Maps the external schema links to the corresponding schema id. That schema id then
 *                            corresponds with a schema in the lookupTable. That schema should represent an
 *                            actual type (integer, number, string, boolean, object, List[integer], List[number],
 *                            List[string], List[boolean], List[object], or even nested lists).
 */
case class SchemaLookup(lookupTable: Map[RootId, Schema] = Map.empty,
                        objectMap: Map[AbsoluteId, ObjectElExt] = Map.empty,
                        enumMap: Map[AbsoluteId, EnumEl] = Map.empty,
                        classReps: Map[AbsoluteId, ClassRep] = Map.empty,
                        externalSchemaLinks: Map[String, RootId] = Map.empty) {

  def map(f: SchemaLookup => SchemaLookup): SchemaLookup = f(this)


  def lookupSchema(id: Id): Schema = {

    val absoluteId = SchemaUtil.asAbsoluteId(id)

    @tailrec
    def fragmentSearch(schema: Schema, fragmentPath: List[String]): Schema = {
      fragmentPath match {
        case Nil       => schema
        case fr :: frs =>
          schema match {
            case fragmentedSchema: FragmentedSchema => fragmentSearch(fragmentedSchema.fragments(fr), frs)
            case _                                  => sys.error(s"Cannot follow the following fragment path: ${absoluteId.id}")
          }
      }
    }

    fragmentSearch(lookupTable(absoluteId.rootPart), absoluteId.fragments)

  }


  def rootIdAsTypedClassReference(rootId: RootId): TypedClassReference = schemaAsClassReference(lookupSchema(rootId)).asTypedClassReference


  /**
   * It's the given schema that tells us what kind of class pointer we'll get.
   */
  def schemaAsClassReference(schema: Schema, types: Map[String, TypedClassReference] = Map.empty): ClassPointer = {

    schema match {
      case objEl: ObjectEl            =>
        val classReference = classReps(SchemaUtil.asAbsoluteId(schema.id)).classRef
        if (types.isEmpty) classReference
        else TypedClassReference(classReference, types)
      case genObjEl: GenericObjectEl  => GenericClassPointer(genObjEl.typeVariable)
      case arrEl: ArrayEl             =>
        arrEl.items match {
          case genObjEl: GenericObjectEl => ListClassReference(genObjEl.typeVariable)
          case itemsSchema               => ListClassReference.typed(schemaAsClassReference(arrEl.items))
        }
      case stringEl: StringEl         => StringClassReference()
      case numberEl: NumberEl         => DoubleClassReference()
      case integerEl: IntegerEl       => LongClassReference()
      case booleanEl: BooleanEl       => BooleanClassReference()
      case schemaRef: SchemaReference =>
        schemaAsClassReference(
          lookupSchema(schemaRef.refersTo),
          schemaRef.genericTypes.mapValues(schemaAsClassReference(_, types).asTypedClassReference)
        )
      case enumEl: EnumEl             =>
        if (enumEl.choices.size == 1) StringClassReference() // Probably a "type" discriminator field.
        else {
          ClassReferenceBuilder(SchemaUtil.asAbsoluteId(schema.id))
        }
      case otherSchema                => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a class representation.")
    }

  }

}
