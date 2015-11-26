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

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.jsonschemaparser._
import io.atomicbits.scraml.jsonschemaparser.model._

/**
 * Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io).
 */
object SchemaClassRepAssembler {

  type CanonicalMap = Map[AbsoluteId, ClassRep]


  def deduceClassReps(schemaLookup: SchemaLookup)(implicit lang: Language): SchemaLookup = {

    val withCanonicals = deduceCanonicalNames(schemaLookup)

    val withEnumClassReps = addEnums(withCanonicals)

    val withCaseClassFields = addCaseClassFields(withEnumClassReps)

    val withClassHierarchy = addParentChildRelations(withCaseClassFields)

    withClassHierarchy
  }


  def addEnums(schemaLookup: SchemaLookup): SchemaLookup = {

    val enumClassReps =
      schemaLookup.enumMap.filter {
        case (id, enumEl) => enumEl.choices.size > 1
      }.map {
        case (id, enumEl) => (id, EnumValuesClassRep(classRef = ClassReferenceBuilder(id), values = enumEl.choices))
      }

    schemaLookup.copy(classReps = enumClassReps ++ schemaLookup.classReps)
  }


  /**
   * @param schemaLookup: The schema lookup
   * @return A map containing the class representation for each absolute ID.
   */
  def deduceCanonicalNames(schemaLookup: SchemaLookup)(implicit lang: Language): SchemaLookup = {

    def jsObjectClassRep: ClassRep =
      lang match {
        case Scala => ClassRep(JsObjectClassReference())
        case Java  => ClassRep(JsonNodeClassReference())
      }

    val canonicalMap: CanonicalMap =
      schemaLookup.objectMap.map {
        case (id: AbsoluteId, obj: ObjectElExt) =>
          if (obj.properties.isEmpty && !obj.hasChildren && !obj.hasParent) id -> jsObjectClassRep
          else id -> ClassRep(ClassReferenceBuilder(id).copy(typeVariables = obj.typeVariables))
      }

    schemaLookup.copy(classReps = canonicalMap)
  }


  def addCaseClassFields(schemaLookup: SchemaLookup)(implicit lang: Language): SchemaLookup = {

    def schemaAsField(property: (String, Schema), requiredFields: List[String]): Field = {

      val (propertyName, schema) = property

      schema match {
        case enumField: EnumEl              =>
          val required = requiredFields.contains(propertyName) || enumField.required
          Field(propertyName, schemaLookup.schemaAsClassReference(enumField), required)
        case objField: AllowedAsObjectField =>
          val required = requiredFields.contains(propertyName) || objField.required
          Field(propertyName, schemaLookup.schemaAsClassReference(objField), required)
        case noObjectField                  =>
          sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
      }

    }

    val canonicalMapWithCaseClassFields =
      schemaLookup.classReps map { idAndClassRep =>
        val (id, classRep) = idAndClassRep

        schemaLookup.objectMap.get(id) match {
          case Some(objectElExt) =>
            val fields: List[Field] = objectElExt.properties.toList.map(schemaAsField(_, objectElExt.requiredFields))

            val classRepWithFields = classRep.withFields(fields)

            (id, classRepWithFields)

          case None =>
            assert(classRep.isInstanceOf[EnumValuesClassRep])
            idAndClassRep
        }
      }

    schemaLookup.copy(classReps = canonicalMapWithCaseClassFields)
  }


  def addParentChildRelations(schemaLookup: SchemaLookup): SchemaLookup = {

    def updateParentAndChildren(objectEl: ObjectElExt, classRp: ClassRep): Map[AbsoluteId, ClassRep] = {

      val typeDiscriminator = objectEl.typeDiscriminator.getOrElse("type")

      val childClassReps =
        objectEl.children map { childId =>

          val childObjectEl = schemaLookup.objectMap(childId)
          val childClassRepWithParent = schemaLookup.classReps(childId).withParent(classRp.classRef)

          val childClassRepWithParentAndJsonInfo =
            childObjectEl.typeDiscriminatorValue.map { typeDiscriminatorValue =>
              childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, Some(typeDiscriminatorValue)))
            } getOrElse childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

          (childId, childClassRepWithParentAndJsonInfo)
        }

      // We assume there can be intermediary levels in the hierarchy.
      val classRepWithParent =
        objectEl.parent map { parentId =>
          classRp.withParent(ClassReferenceBuilder(parentId))
        } getOrElse classRp

      val classRepWithParentAndChildren = classRepWithParent.withChildren(objectEl.children.map(ClassReferenceBuilder(_)))

      val classRepWithParentAndChildrenAndJsonTypeInfo =
        classRepWithParentAndChildren.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

      ((objectEl.id, classRepWithParentAndChildrenAndJsonTypeInfo) :: childClassReps) toMap
    }


    schemaLookup.classReps.foldLeft(schemaLookup) { (lookUp, idWithClassRep) =>
      val (id, classRep) = idWithClassRep

      schemaLookup.objectMap.get(id) match {
        case Some(objectEl) =>
          val classRepsWithParentAndChildren: Map[AbsoluteId, ClassRep] =
            objectEl.selection match {
              case Some(OneOf(selection)) => updateParentAndChildren(objectEl, classRep)
              case Some(AnyOf(selection)) => Map.empty // We only support OneOf for now.
              case Some(AllOf(selection)) => Map.empty // We only support OneOf for now.
              case _                      => Map.empty
            }

          lookUp.copy(classReps = lookUp.classReps ++ classRepsWithParentAndChildren)

        case None => lookUp

      }
    }

  }

}


object ClassReferenceBuilder {

  def apply(origin: AbsoluteId): ClassReference = {

    val hostPathReversed = origin.hostPath.reverse
    val relativePath = origin.rootPath.dropRight(1)
    val originalFileName = origin.rootPath.takeRight(1).head
    val fragmentPath = origin.fragments

    // E.g. when the origin is: http://atomicbits.io/api/schemas/myschema.json#/definitions/schema2
    // then:
    // hostPathReversed = List("io", "atomicbits")
    // relativePath = List("api", "schemas")
    // originalFileName = "myschema.json"
    // fragmentPath = List("definitions", "schema2")

    val classBaseName = CleanNameUtil.cleanClassNameFromFileName(originalFileName)
    val path = hostPathReversed ++ relativePath
    val fragment = fragmentPath

    val className = fragment.foldLeft(classBaseName) { (classNm, fragmentPart) =>
      s"$classNm${CleanNameUtil.cleanClassName(fragmentPart)}"
    }

    ClassReference(name = className, packageParts = path)
  }

}
