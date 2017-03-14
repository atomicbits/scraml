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

package io.atomicbits.scraml.generator.platform.javajackson

import java.io.File

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._
import Platform._
import io.atomicbits.scraml.generator.codegen.GenerationAggr

/**
  * Created by peter on 10/01/17.
  */
object JavaJackson extends Platform with CleanNameTools {

  implicit val platform = JavaJackson

  override def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference = {

    classPointer match {
      case classReference: ClassReference => classReference
      case ArrayClassReference(arrayType) =>
        ClassReference(name = arrayType.native.name, packageParts = arrayType.native.safePackageParts, arrayType = Some(arrayType.native))
      case StringClassReference =>
        ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)
      case ByteClassReference =>
        ClassReference(name = "byte", packageParts = List.empty, predef = true)
      case BinaryDataClassReference =>
        ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "jdsl"), library = true)
      case FileClassReference =>
        ClassReference(name = "File", packageParts = List("java", "io"), library = true)
      case InputStreamClassReference =>
        ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)
      case JsObjectClassReference =>
        ClassReference(name = "JsonNode", packageParts = List("com", "fasterxml", "jackson", "databind"), library = true)
      case JsValueClassReference =>
        ClassReference(name = "JsonNode", packageParts = List("com", "fasterxml", "jackson", "databind"), library = true)
      case BodyPartClassReference =>
        ClassReference(name = "BodyPart", packageParts = List("io", "atomicbits", "scraml", "jdsl"), library = true)
      case LongClassReference(primitive) =>
        if (primitive) {
          ClassReference(name = "long", packageParts = List("java", "lang"), predef = true)
        } else {
          ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
        }
      case DoubleClassReference(primitive) =>
        if (primitive) {
          ClassReference(name = "double", packageParts = List("java", "lang"), predef = true)
        } else {
          ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
        }
      case BooleanClassReference(primitive) =>
        if (primitive) {
          ClassReference(name = "boolean", packageParts = List("java", "lang"), predef = true)
        } else {
          ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
        }
      case ListClassReference(typeParamValue) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = Map(typeParameter -> typeParamValue)
        ClassReference(
          name            = "List",
          packageParts    = List("java", "util"),
          typeParameters  = List(typeParameter),
          typeParamValues = typeParamValues,
          library         = true
        )
      case typeParameter: TypeParameter =>
        ClassReference(name = typeParameter.name, predef = true, isTypeParameter = true)
    }
  }

  override def implementingInterfaceReference(classReference: ClassReference): ClassReference =
    ClassReference(
      name         = s"${classReference.name}Impl",
      packageParts = classReference.packageParts
    )

  override def classDefinition(classPointer: ClassPointer, fullyQualified: Boolean = false): String = {
    val classReference = classPointer.native

    val typedClassDefinition =
      if (classReference.typeParameters.isEmpty) {
        classReference.name
      } else {
        val typeParametersOrValues = classReference.typeParameters.map { typeParam =>
          classReference.typeParamValues
            .get(typeParam)
            .map { classPointer =>
              if (fullyQualified) classPointer.native.fullyQualifiedClassDefinition
              else classPointer.native.classDefinition
            }
            .getOrElse(typeParam.name)
        }
        s"${classReference.name}<${typeParametersOrValues.mkString(",")}>"
      }

    val arrayedClassDefinition =
      if (classReference.isArray) s"$typedClassDefinition[]"
      else typedClassDefinition

    if (fullyQualified) {
      val parts = safePackageParts(classPointer) :+ arrayedClassDefinition
      parts.mkString(".")
    } else {
      arrayedClassDefinition
    }
  }

  override def className(classPointer: ClassPointer): String = classPointer.native.name

  override def packageName(classPointer: ClassPointer): String = safePackageParts(classPointer).mkString(".")

  override def fullyQualifiedName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ className(classPointer)
    parts.mkString(".")
  }

  override def safePackageParts(classPointer: ClassPointer): List[String] = {
    classPointer.native.packageParts.map(part => escapeJavaKeyword(cleanPackageName(part), "esc"))
  }

  override def safeFieldName(field: Field): String = {
    val cleanName = cleanFieldName(field.fieldName)
    escapeJavaKeyword(cleanName)
  }

  override def fieldDeclarationWithDefaultValue(field: Field): String = fieldDeclaration(field)

  override def fieldDeclaration(field: Field): String = {
    s"${classDefinition(field.classPointer)} ${safeFieldName(field)}"
  }

  override def importStatements(targetClassReference: ClassReference, dependencies: Set[ClassPointer] = Set.empty): Set[String] = {
    val ownPackage = targetClassReference.packageName

    def collectTypeImports(collected: Set[String], classPtr: ClassPointer): Set[String] = {

      def importFromClassReference(classRef: ClassReference): Option[String] = {
        if (classRef.isArray) {
          importFromClassReference(classRef.arrayType.get)
        } else {
          if (classRef.packageName != ownPackage && !classRef.predef) Some(s"import ${classRef.fullyQualifiedName};")
          else None
        }
      }

      val classReference = classPtr.native
      val collectedWithClassRef =
        importFromClassReference(classReference).map(classRefImport => collected + classRefImport).getOrElse(collected)

      classReference.typeParamValues.values.toSet.foldLeft(collectedWithClassRef)(collectTypeImports)
    }

    val targetClassImports: Set[String] = collectTypeImports(Set.empty, targetClassReference)

    val dependencyImports: Set[String] = dependencies.foldLeft(targetClassImports)(collectTypeImports)

    dependencyImports
  }

  override def toSourceFile(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr =
    PojoGenerator.generate(generationAggr, toClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr =
    InterfaceGenerator.generate(generationAggr, toInterfaceDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr =
    EnumGenerator.generate(generationAggr, enumDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr =
    ClientClassGenerator.generate(generationAggr, clientClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr =
    ResourceClassGenerator.generate(generationAggr, resourceClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition): GenerationAggr =
    HeaderSegmentClassGenerator.generate(generationAggr, headerSegmentClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, unionClassDefinition: UnionClassDefinition): GenerationAggr =
    UnionClassGenerator.generate(generationAggr, unionClassDefinition)

  override def classFileExtension: String = "java"

  override def toFilePath(classPointer: ClassPointer): String = {
    classPointer match {
      case classReference: ClassReference =>
        s"${classReference.safePackageParts.mkString(File.separator)}${File.separator}${classReference.name}.$classFileExtension"
      case _ => sys.error(s"Cannot create a file path from a class pointer that is not a class reference!")
    }
  }

  def escapeJavaKeyword(someName: String, escape: String = "$"): String = {

    val javaReservedWords =
      List(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while"
      )

    javaReservedWords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"$name$escape"
      else name
    }

  }

}
