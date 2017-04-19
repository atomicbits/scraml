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

package io.atomicbits.scraml.generator.platform.scalaplay

import java.io.File
import java.nio.file.{ Path, Paths }

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._
import Platform._
import io.atomicbits.scraml.generator.codegen.GenerationAggr

/**
  * Created by peter on 10/01/17.
  */
object ScalaPlay extends Platform with CleanNameTools {

  implicit val platform = ScalaPlay

  val dslBasePackageParts: List[String] = List("io", "atomicbits", "scraml", "dsl")

  override def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference = {
    classPointer match {
      case classReference: ClassReference => classReference
      case ArrayClassPointer(arrayType) =>
        val typeParameter = TypeParameter("T")
        ClassReference(name = "Array", typeParameters = List(typeParameter), typeParamValues = List(arrayType), predef = true)
      case StringClassPointer =>
        ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)
      case ByteClassPointer =>
        ClassReference(name = "Byte", packageParts = List("scala"), predef = true)
      case BinaryDataClassPointer =>
        ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "dsl"), library = true)
      case FileClassPointer =>
        ClassReference(name = "File", packageParts = List("java", "io"), library = true)
      case InputStreamClassPointer =>
        ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)
      case JsObjectClassPointer =>
        ClassReference(name = "JsObject", packageParts = List("play", "api", "libs", "json"), library = true)
      case JsValueClassPointer =>
        ClassReference(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)
      case BodyPartClassPointer =>
        ClassReference(name = "BodyPart", packageParts = List("io", "atomicbits", "scraml", "dsl"), library = true)
      case LongClassPointer(primitive) =>
        ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
      case DoubleClassPointer(primitive) =>
        ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
      case BooleanClassPointer(primitive) =>
        ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
      case ListClassPointer(typeParamValue) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = List(typeParamValue)
        ClassReference(name = "List", typeParameters = List(typeParameter), typeParamValues = typeParamValues, predef = true)
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

    val classDef =
      (classReference.typeParameters, classReference.typeParamValues) match {
        case (Nil, _) => classReference.name
        case (tps, Nil) =>
          val typeParameterNames = tps.map(_.name)
          s"${classReference.name}[${typeParameterNames.mkString(",")}]"
        case (tps, tpvs) if tps.size == tpvs.size =>
          val typeParameterValueClassDefinitions =
            tpvs.map { classPointer =>
              if (fullyQualified) classPointer.native.fullyQualifiedClassDefinition
              else classPointer.native.classDefinition
            }
          s"${classReference.name}[${typeParameterValueClassDefinitions.mkString(",")}]"
        case (tps, tpvs) =>
          val message =
            s"""
               |The following class definition has a different number of type parameter 
               |values than there are type parameters: 
               |$classPointer
             """.stripMargin
          sys.error(message)
      }

    if (fullyQualified) {
      val parts = safePackageParts(classPointer) :+ classDef
      parts.mkString(".")
    } else {
      classDef
    }
  }

  override def className(classPointer: ClassPointer): String = classPointer.native.name

  override def packageName(classPointer: ClassPointer): String = safePackageParts(classPointer).mkString(".")

  override def fullyQualifiedName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ className(classPointer)
    parts.mkString(".")
  }

  override def safePackageParts(classPointer: ClassPointer): List[String] =
    classPointer.native.packageParts.map(escapeScalaKeyword(_, "esc"))

  override def safeFieldName(field: Field): String = {
    val cleanName = cleanFieldName(field.fieldName)
    escapeScalaKeyword(cleanName)
  }

  override def fieldDeclarationWithDefaultValue(field: Field): String = {
    if (field.required) {
      s"${safeFieldName(field)}: ${classDefinition(field.classPointer)}"
    } else {
      s"${safeFieldName(field)}: Option[${classDefinition(field.classPointer)}] = None"
    }
  }

  override def fieldDeclaration(field: Field): String = {
    if (field.required) s"${safeFieldName(field)}: ${classDefinition(field.classPointer)}"
    else s"${safeFieldName(field)}: Option[${classDefinition(field.classPointer)}]"
  }

  def fieldFormatUnlift(field: Field): String =
    if (field.required)
      s""" (__ \\ "${field.fieldName}").format[${classDefinition(field.classPointer)}]"""
    else
      s""" (__ \\ "${field.fieldName}").formatNullable[${classDefinition(field.classPointer)}]"""

  override def importStatements(targetClassReference: ClassPointer, dependencies: Set[ClassPointer] = Set.empty): Set[String] = {
    val ownPackage = targetClassReference.packageName

    def collectTypeImports(collected: Set[String], classPtr: ClassPointer): Set[String] = {

      def importFromClassReference(classRef: ClassReference): Option[String] = {
        if (classRef.packageName != ownPackage && !classRef.predef) Some(s"import ${classRef.fullyQualifiedName}")
        else None
      }

      val classReference = classPtr.native
      val collectedWithClassRef =
        importFromClassReference(classReference).map(classRefImport => collected + classRefImport).getOrElse(collected)

      classReference.typeParamValues.foldLeft(collectedWithClassRef)(collectTypeImports)
    }

    val targetClassImports: Set[String] = collectTypeImports(Set.empty, targetClassReference)

    val dependencyImports: Set[String] = dependencies.foldLeft(targetClassImports)(collectTypeImports)

    dependencyImports
  }

  override def toSourceFile(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr =
    CaseClassGenerator.generate(generationAggr, toClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr =
    TraitGenerator.generate(generationAggr, toInterfaceDefinition)

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

  override def classFileExtension: String = "scala"

  override def toFilePath(classPointer: ClassPointer): Path = {
    classPointer match {
      case classReference: ClassReference =>
        val parts = classReference.safePackageParts :+ s"${classReference.name}.$classFileExtension"
        Paths.get("", parts: _*) // This results in a relative path both on Windows as on Linux/Mac
      case _ => sys.error(s"Cannot create a file path from a class pointer that is not a class reference!")
    }
  }

  def escapeScalaKeyword(someName: String, escape: String = "$"): String = {
    val scalaReservedwords =
      List(
        "Byte",
        "Short",
        "Char",
        "Int",
        "Long",
        "Float",
        "Double",
        "Boolean",
        "Unit",
        "String",
        "abstract",
        "case",
        "catch",
        "class",
        "def",
        "do",
        "else",
        "extends",
        "false",
        "final",
        "finally",
        "for",
        "forSome",
        "if",
        "implicit",
        "import",
        "lazy",
        "match",
        "new",
        "null",
        "object",
        "override",
        "package",
        "private",
        "protected",
        "return",
        "sealed",
        "super",
        "this",
        "throw",
        "trait",
        "try",
        "true",
        "type",
        "val",
        "var",
        "while",
        "with",
        "yield"
      )

    scalaReservedwords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"$name$escape"
      else name
    }

  }

}
