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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.lookup.{SchemaUtil, SchemaLookup}
import io.atomicbits.scraml.jsonschemaparser.{TypeClassRep, PlainClassRep, ClassRep, AbsoluteId}
import io.atomicbits.scraml.jsonschemaparser.model._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Created by peter on 21/07/15. 
 */
object TypeGenerator {

  def schemaAsClassRep(schema: Schema, schemaLookup: SchemaLookup): Option[ClassRep] = {

    schema match {
      case objEl: ObjectEl            =>
        val absoluteId = SchemaUtil.asAbsoluteId(schema.id)
        Some(schemaLookup.canonicalNames(absoluteId))
      case arrEl: ArrayEl             =>
        schemaAsClassRep(arrEl.items, schemaLookup) map { typeClassRep =>
          TypeClassRep("List", List(typeClassRep))
        }
      case stringEl: StringEl         => Some(PlainClassRep("String"))
      case numberEl: NumberEl         => Some(PlainClassRep("Double"))
      case integerEl: IntegerEl       => Some(PlainClassRep("Int"))
      case booleanEl: BooleanEl       => Some(PlainClassRep("Boolean"))
      case schemaRef: SchemaReference => schemaAsClassRep(schemaLookup.lookupSchema(schemaRef.refersTo), schemaLookup)
      case enumEl: EnumEl             =>
        val absoluteId = SchemaUtil.asAbsoluteId(schema.id)
        Some(schemaLookup.canonicalNames(absoluteId))
      case otherSchema                => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a class representation.")
    }

  }


  def schemaAsType(schema: Schema, schemaLookup: SchemaLookup, c: whitebox.Context): c.universe.Tree = {

    import c.universe._

    def schemaAsTypeHelper(schema: Schema): Tree = {

      schema match {
        case objEl: ObjectEl            =>
          val absoluteId = SchemaUtil.asAbsoluteId(schema.id)
          classRepAsType(schemaLookup.canonicalNames(absoluteId), c)
        case arrEl: ArrayEl             =>
          val q"val foo: $listType" = q"val foo: List[${schemaAsTypeHelper(arrEl.items)}]"
          listType
        case stringEl: StringEl         =>
          classRepAsType(PlainClassRep("String"), c)
        case numberEl: NumberEl         =>
          classRepAsType(PlainClassRep("Double"), c)
        case integerEl: IntegerEl       =>
          classRepAsType(PlainClassRep("Int"), c)
        case booleanEl: BooleanEl       =>
          classRepAsType(PlainClassRep("Boolean"), c)
        case schemaRef: SchemaReference => schemaAsTypeHelper(schemaLookup.lookupSchema(schemaRef.refersTo))
        case enumEl: EnumEl             =>
          val absoluteId = SchemaUtil.asAbsoluteId(schema.id)
          classRepAsType(schemaLookup.canonicalNames(absoluteId), c)
        case otherSchema                => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a List type parameter.")
      }
    }

    schemaAsTypeHelper(schema)

  }


  def schemaAsField(property: (String, Schema), requiredFields: List[String], schemaLookup: SchemaLookup, c: whitebox.Context) = {

    import c.universe._

    def expandFieldName(fieldName: TermName, typeName: c.universe.Tree, required: Boolean): Tree = {
      if (required) {
        q"val $fieldName: $typeName"
      } else {
        q"val $fieldName: Option[$typeName]"
      }
    }

    val (propertyName, schema) = property

    val absoluteId = schema.id match {
      case absId: AbsoluteId => absId
      case _                 => sys.error("All schema references must be absolute for case class generation.")
    }

    val field =
      schema match {
        case objField: AllowedAsObjectField =>
          val required = requiredFields.contains(propertyName) || objField.required
          expandFieldName(TermName(propertyName), schemaAsType(objField, schemaLookup, c), required)
        case noObjectField                  =>
          sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
      }

    field
  }


  def classRepAsType(classRep: ClassRep, c: whitebox.Context): c.universe.Tree = {

    import c.universe._

    classRep match {
      case plainClassRep: PlainClassRep =>
        // Don't ask my why we have to do it this way to get a TypeName into a Tree without ending up with strings later on.
        val className = TypeName(plainClassRep.name)
        val q"val foo: $classType" = q"val foo: $className"
        classType
      case typeClassRep: TypeClassRep   =>
        val className = TypeName(typeClassRep.name)
        val q"val foo: $typedClassType" = q"val foo: ${className}[..${typeClassRep.types.map(classRepAsType(_, c))}]"
        typedClassType
    }

  }

}
