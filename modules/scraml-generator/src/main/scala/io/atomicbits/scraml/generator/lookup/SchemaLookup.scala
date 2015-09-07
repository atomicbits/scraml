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


  def rootIdAsClassRep(rootId: RootId): ClassRep = schemaAsClassRep(lookupSchema(rootId))


  def schemaAsClassRep(schema: Schema): ClassRep = {

    schema match {
      case objEl: ObjectEl            => classReps(SchemaUtil.asAbsoluteId(schema.id))
      case arrEl: ArrayEl             => ListClassRep(schemaAsClassRep(arrEl.items))
      case stringEl: StringEl         => StringClassRep
      case numberEl: NumberEl         => DoubleClassRep
      case integerEl: IntegerEl       => LongClassRep
      case booleanEl: BooleanEl       => BooleanClassRep
      case schemaRef: SchemaReference => schemaAsClassRep(lookupSchema(schemaRef.refersTo))
      case enumEl: EnumEl             =>
        if (enumEl.choices.size == 1) StringClassRep // Probably a "type" discriminator field.
        else classReps(SchemaUtil.asAbsoluteId(schema.id))
      case otherSchema                => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a class representation.")
    }

  }


  def schemaAsType(schema: Schema): String = schemaAsClassRep(schema).classDefinition


}
