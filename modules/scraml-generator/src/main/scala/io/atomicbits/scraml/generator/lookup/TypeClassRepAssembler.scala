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
import io.atomicbits.scraml.ramlparser.model.{AbsoluteId, NativeId, RootId, UniqueId}
import io.atomicbits.scraml.ramlparser.model.types._

/**
  * Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io).
  */
class TypeClassRepAssembler(nativeToRootId: NativeId => RootId) {

  type CanonicalMap = Map[AbsoluteId, ClassRep]

  val classReferenceBuilder = new ClassReferenceBuilder(nativeToRootId)


  def deduceClassReps(schemaLookup: TypeLookupTable)(implicit lang: Language): TypeLookupTable = {

    val withCanonicals = deduceCanonicalNames(schemaLookup)

    val withEnumClassReps = addEnums(withCanonicals)

    val withCaseClassFields = addCaseClassFields(withEnumClassReps)

    val withClassHierarchy = addParentChildRelations(withCaseClassFields)

    withClassHierarchy
  }


  def addEnums(schemaLookup: TypeLookupTable): TypeLookupTable = {

    val enumClassReps =
      schemaLookup.enumMap.filter {
        case (id, enumEl) => enumEl.choices.size > 1
      }.map {
        case (id, enumEl) => (id, EnumValuesClassRep(classRef = classReferenceBuilder(id), values = enumEl.choices))
      }

    schemaLookup.copy(classReps = enumClassReps ++ schemaLookup.classReps)
  }


  /**
    * @param lookupTable : The type lookup table
    * @return A map containing the class representation for each absolute ID.
    */
  def deduceCanonicalNames(lookupTable: TypeLookupTable)(implicit lang: Language): TypeLookupTable = {

    def jsObjectClassRep: ClassRep =
      lang match {
        case Scala => ClassRep(JsObjectClassReference())
        case Java  => ClassRep(JsonNodeClassReference())
      }

    val canonicalMap: CanonicalMap =
      lookupTable.objectMap.map {
        case (id: AbsoluteId, obj: ObjectModel) =>
          if (obj.properties.isEmpty && !obj.hasChildren && !obj.hasParent) id -> jsObjectClassRep
          else id -> ClassRep(classReferenceBuilder(id).copy(typeVariables = obj.typeVariables))
      }

    lookupTable.copy(classReps = canonicalMap)
  }


  def addCaseClassFields(schemaLookup: TypeLookupTable)(implicit lang: Language): TypeLookupTable = {

    def schemaAsField(property: (String, Type), requiredFields: List[String]): Field = {

      val (propertyName, schema) = property

      schema match {
        case enumField: EnumType              =>
          val required = requiredFields.contains(propertyName) || enumField.isRequired
          Field(propertyName, typeAsClassReference(enumField, schemaLookup), required)
        case objField: AllowedAsObjectField =>
          val required = requiredFields.contains(propertyName) || objField.isRequired
          Field(propertyName, typeAsClassReference(objField, schemaLookup), required)
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


  def addParentChildRelations(lookupTable: TypeLookupTable): TypeLookupTable = {

    def updateParentAndChildren(objectModel: ObjectModel, classRp: ClassRep): Map[AbsoluteId, ClassRep] = {

      val typeDiscriminator = objectModel.typeDiscriminator.getOrElse("type")

      val childClassReps =
        objectModel.children map { childId =>

          val childObjectEl = lookupTable.objectMap(childId)
          val childClassRepWithParent = lookupTable.classReps(childId).withParent(classRp.classRef)

          val childClassRepWithParentAndJsonInfo =
            childObjectEl.typeDiscriminatorValue.map { typeDiscriminatorValue =>
              childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, Some(typeDiscriminatorValue)))
            } getOrElse childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

          (childId, childClassRepWithParentAndJsonInfo)
        }

      // We assume there can be intermediary levels in the hierarchy.
      val classRepWithParent =
      objectModel.parent map { parentId =>
        classRp.withParent(classReferenceBuilder(parentId))
      } getOrElse classRp

      val classRepWithParentAndChildren = classRepWithParent.withChildren(objectModel.children.map(classReferenceBuilder(_)))

      val classRepWithParentAndChildrenAndJsonTypeInfo =
        classRepWithParentAndChildren.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

      ((objectModel.id, classRepWithParentAndChildrenAndJsonTypeInfo) :: childClassReps) toMap
    }


    lookupTable.classReps.foldLeft(lookupTable) { (lookUp, idWithClassRep) =>
      val (id, classRep) = idWithClassRep

      lookupTable.objectMap.get(id) match {
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


  /**
    * It's the given schema that tells us what kind of class pointer we'll get.
    */
  def typeAsClassReference(ttype: Type, lookupTable: TypeLookupTable, types: Map[String, TypedClassReference] = Map
    .empty)(implicit lang: Language): ClassPointer = {

    ttype match {
      case objectType: ObjectType               =>
        val classReference = lookupTable.classReps(TypeUtils.asAbsoluteId(ttype.id)).classRef
        if (types.isEmpty) classReference
        else TypedClassReference(classReference, types)
      case genericObjectType: GenericObjectType => GenericClassPointer(genericObjectType.typeVariable)
      case arrayType: ArrayType                 =>
        arrayType.items match {
          case genericObjectType: GenericObjectType => ListClassReference(genericObjectType.typeVariable)
          case itemsType                            => ListClassReference.typed(typeAsClassReference(arrayType.items, lookupTable))
        }
      case stringType: StringType               => StringClassReference()
      case numberType: NumberType               => DoubleClassReference(numberType.isRequired)
      case integerType: IntegerType             => LongClassReference(integerType.isRequired)
      case booleanType: BooleanType             => BooleanClassReference(booleanType.isRequired)
      case typeReference: TypeReference         =>
        typeAsClassReference(
          lookupTable.lookup(typeReference.refersTo),
          lookupTable,
          typeReference.genericTypes.mapValues(typeAsClassReference(_, lookupTable, types).asTypedClassReference)
        )
      case enumType: EnumType                   =>
        if (enumType.choices.size == 1) StringClassReference() // Probably a "type" discriminator field.
        else classReferenceBuilder(TypeUtils.asUniqueId(ttype.id))
      case unknownType                          =>
        sys.error(s"Cannot transform schema with id ${unknownType.id} to a class representation.")
    }

  }

}


class ClassReferenceBuilder(nativeToRootId: NativeId => RootId) {

  def apply(uniqueId: UniqueId): ClassReference = {

    val origin: AbsoluteId =
      uniqueId match {
        case absoluteId: AbsoluteId => absoluteId
        case nativeId: NativeId     => nativeToRootId(nativeId).asInstanceOf[AbsoluteId]
      }

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
