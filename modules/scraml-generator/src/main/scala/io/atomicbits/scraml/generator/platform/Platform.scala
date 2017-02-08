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

package io.atomicbits.scraml.generator.platform

import java.io.File

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{
  ArrayTypeReference,
  BooleanType,
  CanonicalName,
  DateType,
  FileType,
  GenericReferrable,
  IntegerType,
  NonPrimitiveTypeReference,
  NullType,
  NumberType,
  StringType,
  TypeReference,
  TypeParameter => CanonicalTypeParameter
}
import io.atomicbits.scraml.generator.typemodel._

/**
  * Created by peter on 10/01/17.
  */
trait Platform {

  def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference

  def interfaceReference(classReference: ClassReference): ClassReference

  def classDefinition(classPointer: ClassPointer): String

  def className(classPointer: ClassPointer): String

  def packageName(classPointer: ClassPointer): String

  def fullyQualifiedName(classPointer: ClassPointer): String

  def safePackageParts(classPointer: ClassPointer): List[String]

  def classDefinitionString(classPointer: ClassPointer): String

  def safeFieldName(field: Field): String

  def fieldExpression(field: Field): String

  def importStatements(targetClassReference: ClassReference, dependencies: Set[ClassPointer] = Set.empty): Set[String]

  def toSourceFile(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition): GenerationAggr

  def toSourceFile(generationAggr: GenerationAggr, unionClassDefinition: UnionClassDefinition): GenerationAggr

  def classFileExtension: String

  /**
    * Transforms a given class reference to a file path. The given class reference already has clean package and class names.
    *
    * @param classReference The class reference for which a file path is generated.
    * @return The relative file name for the given class.
    */
  def classReferenceToFilePath(classReference: ClassReference): String = {
    s"${classReference.packageParts.mkString(File.separator)}${File.separator}${classReference.name}.$classFileExtension"
  }

}

object Platform {

  def typeReferenceToClassPointer(typeReference: TypeReference, primitive: Boolean = false): ClassPointer = {

    def customClassReference(canonicalName: CanonicalName, genericTypes: Map[CanonicalTypeParameter, GenericReferrable]): ClassReference = {
      val generics: Map[TypeParameter, ClassPointer] =
        genericTypes.map {
          case (typeParameter, genericReferrable) =>
            val tParam = TypeParameter(typeParameter.name)
            val typeRef =
              genericReferrable match {
                case typeReference: TypeReference => typeReferenceToClassPointer(typeReference)
                case CanonicalTypeParameter(paramName) =>
                  sys.error(s"Didn't expect a type parameter when constructing a custom class reference at this stage.")
              }
            tParam -> typeRef
        }
      val typeParameters = generics.keys.toList // ToDo: we lost any order of the type parameters, fix this in NonPrimitiveTypeReference!
      ClassReference(
        name            = canonicalName.name,
        packageParts    = canonicalName.packagePath,
        typeParameters  = typeParameters,
        typeParamValues = generics
      )
    }

    typeReference match {
      case BooleanType        => BooleanClassReference(primitive)
      case StringType         => StringClassReference
      case IntegerType        => LongClassReference(primitive)
      case NumberType         => DoubleClassReference(primitive)
      case NullType           => StringClassReference // not sure what we have to do in this case
      case FileType           => FileClassReference
      case dateType: DateType => StringClassReference // ToDo: support date types
      case ArrayTypeReference(genericType) =>
        genericType match {
          case typeReference: TypeReference =>
            val classPointer = typeReferenceToClassPointer(typeReference)
            ListClassReference(classPointer)
          case CanonicalTypeParameter(paramName) => TypeParameter(paramName)
        }
      case NonPrimitiveTypeReference(refers, genericTypes) => customClassReference(refers, genericTypes)
      case unexpected                                      => sys.error(s"Didn't expect type reference in generator: $unexpected")
    }
  }

  def typeReferenceToNonPrimitiveCanonicalName(typeReference: TypeReference): Option[CanonicalName] = {
    Some(typeReference).collect {
      case NonPrimitiveTypeReference(refers, genericTypes) => refers
    }
  }

  def typeReferenceToClassReference(typeReference: TypeReference): Option[ClassReference] = {
    Some(typeReferenceToClassPointer(typeReference)).collect {
      case classReference: ClassReference => classReference
    }
  }

  implicit class PlatformClassPointerOps(val classPointer: ClassPointer) {

    def classDefinition(implicit platform: Platform): String = platform.classDefinition(classPointer)

    def packageName(implicit platform: Platform): String = platform.packageName(classPointer)

    def fullyQualifiedName(implicit platform: Platform): String = platform.fullyQualifiedName(classPointer)

    def safePackageParts(implicit platform: Platform): List[String] = platform.safePackageParts(classPointer)

    def canonicalName(implicit platform: Platform): String = platform.classDefinitionString(classPointer)

    def native(implicit platform: Platform): ClassReference = platform.classPointerToNativeClassReference(classPointer)

    def interfaceReference(implicit platform: Platform): ClassReference = platform.interfaceReference(classPointer.native)

  }

  implicit class PlatformFieldOps(val field: Field) {

    def safeFieldName(implicit platform: Platform): String = platform.safeFieldName(field)

    def fieldExpression(implicit platform: Platform): String = platform.fieldExpression(field)
  }

  implicit class PlatformToClassDefinitionOps(val toClassDefinition: TransferObjectClassDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, toClassDefinition)

  }

  implicit class PlatformToInterfaceDefinitionOps(val toInterfaceDefinition: TransferObjectInterfaceDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, toInterfaceDefinition)

  }

  implicit class PlatformEnumDefinitionOps(val enumDefinition: EnumDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, enumDefinition)

  }

  implicit class PlatformClientClassDefinitionOps(val clientClassDefinition: ClientClassDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, clientClassDefinition)

  }

  implicit class PlatformResourceClassDefinitionOps(val resourceClassDefinition: ResourceClassDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, resourceClassDefinition)

  }

  implicit class PlatformHeaderSegmentClassDefinitionOps(val headerSegmentClassDefinition: HeaderSegmentClassDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, headerSegmentClassDefinition)

  }

  implicit class PlatformUnionClassDefinitionOps(val unionClassDefinition: UnionClassDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      platform.toSourceFile(generationAggr, unionClassDefinition)

  }

  implicit class PlatformSourceCodeOps(val sourceCode: SourceDefinition) {

    def toSourceFile(generationAggr: GenerationAggr)(implicit platform: Platform): GenerationAggr =
      sourceCode match {
        case clientClassDefinition: ClientClassDefinition =>
          new PlatformClientClassDefinitionOps(clientClassDefinition).toSourceFile(generationAggr)
        case resourceClassDefinition: ResourceClassDefinition =>
          new PlatformResourceClassDefinitionOps(resourceClassDefinition).toSourceFile(generationAggr)
        case headerSegmentDefinition: HeaderSegmentClassDefinition =>
          new PlatformHeaderSegmentClassDefinitionOps(headerSegmentDefinition).toSourceFile(generationAggr)
        case toClassDefinition: TransferObjectClassDefinition =>
          new PlatformToClassDefinitionOps(toClassDefinition).toSourceFile(generationAggr)
        case toInterfaceDefinition: TransferObjectInterfaceDefinition =>
          new PlatformToInterfaceDefinitionOps(toInterfaceDefinition).toSourceFile(generationAggr)
        case enumDefinition: EnumDefinition =>
          new PlatformEnumDefinitionOps(enumDefinition).toSourceFile(generationAggr)
        case unionClassDefinition: UnionClassDefinition =>
          new PlatformUnionClassDefinitionOps(unionClassDefinition).toSourceFile(generationAggr)
      }

  }

}
