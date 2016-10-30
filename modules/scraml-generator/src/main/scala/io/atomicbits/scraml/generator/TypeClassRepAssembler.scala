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

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.lookup.{TypeLookupTable, TypeUtils}
import io.atomicbits.scraml.ramlparser.model.types._
import io.atomicbits.scraml.ramlparser.model.{AbsoluteId, NativeId, UniqueId}

/**
  * Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io).
  */
object TypeClassRepAssembler {

  type CanonicalMap = Map[AbsoluteId, ClassRep]


  def deduceClassReps(lookupTable: TypeLookupTable)(implicit lang: Language): CanonicalMap = {

    val withCanonicals = deduceCanonicalNames(lookupTable)

    val withEnumClassReps = addEnums(withCanonicals, lookupTable)

    val withCaseClassFields = addClassFields(withEnumClassReps, lookupTable)

    val withClassHierarchy = addParentChildRelations(withCaseClassFields, lookupTable)

    withClassHierarchy
  }


  def addEnums(canonicalMap: CanonicalMap, lookupTable: TypeLookupTable): CanonicalMap = {

    val enumClassReps =
      lookupTable.enumMap.filter {
        case (id, enumEl) => enumEl.choices.size > 1
      }.map {
        case (id, enumEl) => (id, EnumValuesClassRep(classRef = ClassReferenceBuilder(lookupTable)(id), values = enumEl.choices))
      }

    enumClassReps ++ canonicalMap
  }


  /**
    * @param lookupTable : The type lookup table
    * @return A map containing the class representation for each absolute ID.
    */
  def deduceCanonicalNames(lookupTable: TypeLookupTable)(implicit lang: Language): CanonicalMap = {

    def jsObjectClassRep: ClassRep =
      lang match {
        case Scala => ClassRep(JsObjectClassReference())
        case Java  => ClassRep(JsonNodeClassReference())
      }

    val canonicalMap: CanonicalMap =
      lookupTable.objectMap.map {
        case (id: AbsoluteId, obj: ObjectType) =>
          if (obj.properties.isEmpty && !obj.hasChildren && !obj.hasParent) id -> jsObjectClassRep
          else id -> ClassRep(ClassReferenceBuilder(lookupTable)(id).copy(typeParameters = obj.typeParameters.map(TypeParameter(_))))
      }

    canonicalMap
  }


  def addClassFields(canonicalMap: CanonicalMap, lookupTable: TypeLookupTable)(implicit lang: Language): CanonicalMap = {

    def schemaAsField(property: (String, Type), requiredFields: List[String]): Field = {

      val (propertyName, schema) = property

      schema match {
        case enumField: EnumType            =>
          val required = requiredFields.contains(propertyName) || enumField.isRequired
          Field(propertyName, typeAsClassReference(enumField, lookupTable, canonicalMap), required)
        case objField: AllowedAsObjectField =>
          val required = requiredFields.contains(propertyName) || objField.isRequired
          Field(propertyName, typeAsClassReference(objField, lookupTable, canonicalMap), required)
        case noObjectField                  =>
          sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
      }

    }

    val canonicalMapWithCaseClassFields: CanonicalMap =
      canonicalMap map { idAndClassRep =>
        val (id, classRep) = idAndClassRep

        lookupTable.objectMap.get(id) match {
          case Some(objectElExt) =>
            val fields: List[Field] = objectElExt.properties.toList.map(schemaAsField(_, objectElExt.requiredFields))

            val classRepWithFields = classRep.withFields(fields)

            (id, classRepWithFields)

          case None =>
            assert(classRep.isInstanceOf[EnumValuesClassRep])
            idAndClassRep
        }
      }

    canonicalMapWithCaseClassFields
  }


  def addParentChildRelations(canonicalMap: CanonicalMap, lookupTable: TypeLookupTable): CanonicalMap = {

    def updateParentAndChildren(objectType: ObjectType, classRp: ClassRep): Map[AbsoluteId, ClassRep] = {

      val typeDiscriminator = objectType.typeDiscriminator.getOrElse("type")

      val childClassReps =
        objectType.children map { childId =>

          val childObjectEl = lookupTable.objectMap(childId)
          val childClassRepWithParent = canonicalMap(childId).withParent(classRp.classRef)

          val childClassRepWithParentAndJsonInfo =
            childObjectEl.typeDiscriminatorValue.map { typeDiscriminatorValue =>
              childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, Some(typeDiscriminatorValue)))
            } getOrElse childClassRepWithParent.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

          (childId, childClassRepWithParentAndJsonInfo)
        }


      // We assume there can be intermediary levels in the hierarchy.
      val classRepWithParent =
      objectType.parent map { parentId =>
        classRp.withParent(ClassReferenceBuilder(lookupTable)(parentId))
      } getOrElse classRp

      val classRepWithParentAndChildren = classRepWithParent.withChildren(objectType.children.map(ClassReferenceBuilder(lookupTable)(_)))

      val classRepWithParentAndChildrenAndJsonTypeInfo =
        classRepWithParentAndChildren.withJsonTypeInfo(JsonTypeInfo(typeDiscriminator, None))

      val absoluteId = TypeUtils.asAbsoluteId(objectType.id, lookupTable.nativeToAbsoluteId)

      ((absoluteId, classRepWithParentAndChildrenAndJsonTypeInfo) :: childClassReps) toMap
    }


    canonicalMap.foldLeft(canonicalMap) { (updatedCanonicalMap, idWithClassRep) =>
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

          updatedCanonicalMap ++ classRepsWithParentAndChildren

        case None => updatedCanonicalMap

      }
    }

  }


  /**
    * It's the given schema that tells us what kind of class pointer we'll get.
    */
  def typeAsClassReference(ttype: Type,
                           lookupTable: TypeLookupTable,
                           canonicalMap: CanonicalMap,
                           typeVariables: Map[TypeParameter, TypedClassReference] = Map.empty)
                          (implicit lang: Language): ClassPointer = {

    ttype match {
      case objectType: ObjectType               =>
        val classReference = canonicalMap(TypeUtils.asAbsoluteId(ttype.id, lookupTable.nativeToAbsoluteId)).classRef
        if (typeVariables.isEmpty) classReference
        else TypedClassReference(classReference, typeVariables)
      case genericObjectType: GenericObjectType => TypeParameter(genericObjectType.typeVariable)
      case arrayType: ArrayType                 =>
        arrayType.items match {
          case genericObjectType: GenericObjectType => ListClassReference(genericObjectType.typeVariable)
          case itemsType                            =>
            ListClassReference.typed(typeAsClassReference(arrayType.items, lookupTable, canonicalMap))
        }
      case stringType: StringType               => StringClassReference()
      case numberType: NumberType               => DoubleClassReference(numberType.isRequired)
      case integerType: IntegerType             => LongClassReference(integerType.isRequired)
      case booleanType: BooleanType             => BooleanClassReference(booleanType.isRequired)
      case typeReference: TypeReference         =>
        val typeRefTypeVars =
          typeReference.genericTypes.map {
            case (typeParamName, theType) =>
              TypeParameter(typeParamName) -> typeAsClassReference(theType, lookupTable, canonicalMap, typeVariables).asTypedClassReference
          }
        typeAsClassReference(
          lookupTable.lookup(typeReference.refersTo),
          lookupTable,
          canonicalMap,
          typeRefTypeVars
        )
      case enumType: EnumType                   =>
        if (enumType.choices.size == 1) StringClassReference() // Probably a "type" discriminator field.
        else ClassReferenceBuilder(lookupTable)(TypeUtils.asUniqueId(ttype.id))
      case unknownType                          =>
        sys.error(s"Cannot transform schema with id ${unknownType.id} to a class representation.")
    }

  }

}


case class ClassReferenceBuilder(typeLookupTable: TypeLookupTable) {

  def apply(uniqueId: UniqueId): ClassReference = {

    val origin: AbsoluteId =
      uniqueId match {
        case absoluteId: AbsoluteId => absoluteId
        case nativeId: NativeId     => typeLookupTable.nativeToAbsoluteId(nativeId)
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
