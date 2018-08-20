/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.platform

import java.nio.file.Path

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ TypeParameter => ParserTypeParameter }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{
  ArrayTypeReference,
  BooleanType,
  CanonicalName,
  DateOnlyType,
  DateTimeDefaultType,
  DateTimeOnlyType,
  DateTimeRFC2616Type,
  DateType,
  FileType,
  GenericReferrable,
  IntegerType,
  JsonType,
  NonPrimitiveTypeReference,
  NullType,
  NumberType,
  ObjectType,
  StringType,
  TimeOnlyType,
  TypeReference,
  TypeParameter => CanonicalTypeParameter
}
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 10/01/17.
  */
trait Platform {

  def name: String

  def apiBasePackageParts: List[String]
  def apiBasePackage: String = apiBasePackageParts.mkString(".")
  def apiBaseDir: String     = apiBasePackageParts.mkString("/", "/", "")

  def dslBasePackageParts: List[String]
  def dslBasePackage: String = dslBasePackageParts.mkString(".")
  def dslBaseDir: String     = dslBasePackageParts.mkString("/", "/", "")

  def rewrittenDslBasePackage: List[String]

  def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference

  /**
    * The implementing interface reference is the reference to the class (transfer object class) that implements the
    * interface that replaces it in a multiple inheritance relation (and in Scala also in a regular inheritance relation).
    * E.g. AnimalImpl implements Animal --> Here, 'Animal' is the interface where resources and cross referencing inside TO's point to.
    */
  def implementingInterfaceReference(classReference: ClassReference): ClassReference

  /**
    * The definition of a class.
    * E.g. List[String] or List<String> or Element or PagedList<T> or PagedList[Element]
    * or their fully qualified verions
    * E.g. List[String] or java.util.List<String> or io.atomicbits.Element or io.atomicbits.PagedList<T> or io.atomicbits.PagedList[Element]
    */
  def classDefinition(classPointer: ClassPointer, fullyQualified: Boolean = false): String

  def className(classPointer: ClassPointer): String

  def packageName(classPointer: ClassPointer): String

  def fullyQualifiedName(classPointer: ClassPointer): String

  def safePackageParts(classPointer: ClassPointer): List[String]

  def safeFieldName(field: Field): String = safeFieldName(field.fieldName)

  def safeFieldName(fieldName: String): String

  def fieldDeclarationWithDefaultValue(field: Field): String

  def fieldDeclaration(field: Field): String

  def importStatements(targetClassReference: ClassPointer, dependencies: Set[ClassPointer] = Set.empty): Set[String]

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
    * @param classPointer The class reference for which a file path is generated.
    * @return The relative file name for the given class.
    */
  def toFilePath(classPointer: ClassPointer): Path

  /**
    * Platform specific mapping from the generated sourcefiles. Mostly the original sources will be kept, hence the
    * default implementation. Other platforms will map the sources into a single file (e.g. typescript).
    */
  def mapSourceFiles(sources: Set[SourceFile], combinedSourcesFileName: Option[String] = None): Set[SourceFile] = sources

  def reservedKeywords: Set[String]

}

object Platform {

  def typeReferenceToClassPointer(typeReference: TypeReference): ClassPointer = {

    def customClassReference(canonicalName: CanonicalName,
                             genericTypes: List[GenericReferrable],
                             genericTypeParameters: List[ParserTypeParameter]): ClassReference = {
      val generics: List[ClassPointer] =
        genericTypes.map {
          case typeRef: TypeReference => typeReferenceToClassPointer(typeRef)
          case CanonicalTypeParameter(paramName) =>
            sys.error(s"Didn't expect a type parameter when constructing a custom class reference at this stage.")
        }
      val typeParameters = genericTypeParameters.map(tp => TypeParameter(tp.name))

      ClassReference(
        name            = canonicalName.name,
        packageParts    = canonicalName.packagePath,
        typeParameters  = typeParameters,
        typeParamValues = generics
      )
    }

    typeReference match {
      case BooleanType         => BooleanClassPointer(false)
      case StringType          => StringClassPointer
      case JsonType            => JsObjectClassPointer
      case IntegerType         => LongClassPointer(false)
      case NumberType          => DoubleClassPointer(false)
      case NullType            => StringClassPointer // not sure what we have to do in this case
      case FileType            => FileClassPointer
      case DateTimeDefaultType => DateTimeRFC3339ClassPointer
      case DateTimeRFC2616Type => DateTimeRFC2616ClassPointer
      case DateTimeOnlyType    => DateTimeOnlyClassPointer
      case TimeOnlyType        => TimeOnlyClassPointer
      case DateOnlyType        => DateOnlyClassPointer
      case ArrayTypeReference(genericType) =>
        genericType match {
          case typeReference: TypeReference =>
            val classPointer = typeReferenceToClassPointer(typeReference)
            ListClassPointer(classPointer)
          case CanonicalTypeParameter(paramName) =>
            val typeParameter = TypeParameter(paramName)
            ListClassPointer(typeParameter)
        }
      case NonPrimitiveTypeReference(refers, genericTypes, genericTypeParameters) =>
        customClassReference(refers, genericTypes, genericTypeParameters)
      case unexpected => sys.error(s"Didn't expect type reference in generator: $unexpected")
    }
  }

  def typeReferenceToNonPrimitiveCanonicalName(typeReference: TypeReference): Option[CanonicalName] = {
    Some(typeReference).collect {
      case NonPrimitiveTypeReference(refers, genericTypes, genericTypeParameters) => refers
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

    def fullyQualifiedClassDefinition(implicit platform: Platform): String = platform.classDefinition(classPointer, fullyQualified = true)

    def native(implicit platform: Platform): ClassReference = platform.classPointerToNativeClassReference(classPointer)

    def toFilePath(implicit platform: Platform): Path = platform.toFilePath(classPointer)

    def implementingInterfaceReference(implicit platform: Platform): ClassReference =
      platform.implementingInterfaceReference(classPointer.native)

    def importStatements(implicit platform: Platform): Set[String] = platform.importStatements(classPointer)

  }

  implicit class PlatformFieldOps(val field: Field) {

    def safeFieldName(implicit platform: Platform): String = platform.safeFieldName(field)

    def fieldDeclarationWithDefaultValue(implicit platform: Platform): String = platform.fieldDeclarationWithDefaultValue(field)

    def fieldDeclaration(implicit platform: Platform): String = platform.fieldDeclaration(field)

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
