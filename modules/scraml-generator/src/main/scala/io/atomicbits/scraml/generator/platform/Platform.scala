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

  def classDefinition(classPointer: ClassPointer): String

  def className(classPointer: ClassPointer): String

  def packageName(classPointer: ClassPointer): String

  def fullyQualifiedName(classPointer: ClassPointer): String

  def safePackageParts(classPointer: ClassPointer): List[String]

  def canonicalName(classPointer: ClassPointer): String

  def safeFieldName(field: Field): String

  def fieldExpression(field: Field): String

  def importStatements(imports: Set[ClassPointer]): Set[String]

  def toSourceFile(toClassDefinition: TransferObjectClassDefinition): List[SourceFile]

  def toSourceFile(toInterfaceDefinition: TransferObjectInterfaceDefinition): List[SourceFile]

  def toSourceFile(enumDefinition: EnumDefinition): List[SourceFile]

  def toSourceFile(clientClassDefinition: ClientClassDefinition): List[SourceFile]

  def toSourceFile(resourceClassDefinition: ResourceClassDefinition): List[SourceFile]

  def toSourceFile(headerSegmentClassDefinition: HeaderSegmentClassDefinition): List[SourceFile]

  def toSourceFile(unionClassDefinition: UnionClassDefinition): List[SourceFile]

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
          case CanonicalTypeParameter(paramName) =>
            sys.error(s"Didn't expect a type parameter when constructing an array reference at this stage.")
        }
      case NonPrimitiveTypeReference(refers, genericTypes) => customClassReference(refers, genericTypes)
      case unexpected                                      => sys.error(s"Didn't expect type reference in generator: $unexpected")
    }
  }

  implicit class PlatformClassPointerOps(val classPointer: ClassPointer) {

    def classDefinition(implicit platform: Platform): String = platform.classDefinition(classPointer)

    def packageName(implicit platform: Platform): String = platform.packageName(classPointer)

    def fullyQualifiedName(implicit platform: Platform): String = platform.fullyQualifiedName(classPointer)

    def safePackageParts(implicit platform: Platform): List[String] = platform.safePackageParts(classPointer)

    def canonicalName(implicit platform: Platform): String = platform.canonicalName(classPointer)

    def native(implicit platform: Platform): ClassReference = platform.classPointerToNativeClassReference(classPointer)

  }

  implicit class PlatformFieldOps(val field: Field) {

    def safeFieldName(implicit platform: Platform): String = platform.safeFieldName(field)

    def fieldExpression(implicit platform: Platform): String = platform.fieldExpression(field)
  }

  implicit class PlatformToClassDefinitionOps(val toClassDefinition: TransferObjectClassDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(toClassDefinition)

  }

  implicit class PlatformToInterfaceDefinitionOps(val toInterfaceDefinition: TransferObjectInterfaceDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(toInterfaceDefinition)

  }

  implicit class PlatformEnumDefinitionOps(val enumDefinition: EnumDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(enumDefinition)

  }

  implicit class PlatformClientClassDefinitionOps(val clientClassDefinition: ClientClassDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(clientClassDefinition)

  }

  implicit class PlatformResourceClassDefinitionOps(val resourceClassDefinition: ResourceClassDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(resourceClassDefinition)

  }

  implicit class PlatformHeaderSegmentClassDefinitionOps(val headerSegmentClassDefinition: HeaderSegmentClassDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(headerSegmentClassDefinition)

  }

  implicit class PlatformUnionClassDefinitionOps(val unionClassDefinition: UnionClassDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] = platform.toSourceFile(unionClassDefinition)

  }

  implicit class PlatformSourceCodeOps(val sourceCode: SourceDefinition) {

    def toSourceFile(implicit platform: Platform): List[SourceFile] =
      sourceCode match {
        case clientClassDefinition: ClientClassDefinition => new PlatformClientClassDefinitionOps(clientClassDefinition).toSourceFile
        case resourceClassDefinition: ResourceClassDefinition =>
          new PlatformResourceClassDefinitionOps(resourceClassDefinition).toSourceFile
        case headerSegmentDefinition: HeaderSegmentClassDefinition =>
          new PlatformHeaderSegmentClassDefinitionOps(headerSegmentDefinition).toSourceFile
        case toClassDefinition: TransferObjectClassDefinition => new PlatformToClassDefinitionOps(toClassDefinition).toSourceFile
        case toInterfaceDefinition: TransferObjectInterfaceDefinition =>
          new PlatformToInterfaceDefinitionOps(toInterfaceDefinition).toSourceFile
        case enumDefinition: EnumDefinition             => new PlatformEnumDefinitionOps(enumDefinition).toSourceFile
        case unionClassDefinition: UnionClassDefinition => new PlatformUnionClassDefinitionOps(unionClassDefinition).toSourceFile
      }

  }

}
