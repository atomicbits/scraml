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

package io.atomicbits.scraml.generator.platform.scalaplay

import java.nio.file.{ Path, Paths }

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._
import Platform._
import io.atomicbits.scraml.generator.codegen.GenerationAggr

/**
  * Created by peter on 10/01/17.
  */
case class ScalaPlay(apiBasePackageParts: List[String]) extends Platform with CleanNameTools {

  val name: String = "Scala Play"

  implicit val platform: ScalaPlay = this

  val dslBasePackageParts: List[String] = List("io", "atomicbits", "scraml", "dsl", "scalaplay")

  val rewrittenDslBasePackage: List[String] = apiBasePackageParts ++ List("dsl", "scalaplay")

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
        ClassReference(name = "BinaryData", packageParts = rewrittenDslBasePackage, library = true)
      case FileClassPointer =>
        ClassReference(name = "File", packageParts = List("java", "io"), library = true)
      case InputStreamClassPointer =>
        ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)
      case JsObjectClassPointer =>
        ClassReference(name = "JsObject", packageParts = List("play", "api", "libs", "json"), library = true)
      case JsValueClassPointer =>
        ClassReference(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)
      case BodyPartClassPointer =>
        ClassReference(name = "BodyPart", packageParts = rewrittenDslBasePackage, library = true)
      case LongClassPointer(primitive) =>
        ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
      case DoubleClassPointer(primitive) =>
        ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
      case BooleanClassPointer(primitive) =>
        ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
      case DateTimeRFC3339ClassPointer =>
        ClassReference(name = "DateTimeRFC3339", packageParts = rewrittenDslBasePackage, library = true)
      case DateTimeRFC2616ClassPointer =>
        ClassReference(name = "DateTimeRFC2616", packageParts = rewrittenDslBasePackage, library = true)
      case DateTimeOnlyClassPointer =>
        ClassReference(name = "DateTimeOnly", packageParts = rewrittenDslBasePackage, library = true)
      case TimeOnlyClassPointer =>
        ClassReference(name = "TimeOnly", packageParts = rewrittenDslBasePackage, library = true)
      case DateOnlyClassPointer =>
        ClassReference(name = "DateOnly", packageParts = rewrittenDslBasePackage, library = true)
      case ListClassPointer(typeParamValue) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = List(typeParamValue)
        ClassReference(name = "List", typeParameters = List(typeParameter), typeParamValues = typeParamValues, predef = true)
      case typeParameter: TypeParameter =>
        ClassReference(name = typeParameter.name, predef = true, isTypeParameter = true)
      case _: io.atomicbits.scraml.generator.typemodel.PrimitiveClassPointer => ???
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

  override def safeFieldName(fieldName: String): String = {
    val cleanName = cleanFieldName(fieldName)
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

  // format: off
  def fieldFormatUnlift(field: Field, recursiveFields: Set[Field]): String =
    (field.required, recursiveFields.contains(field)) match {
      case (true, false) =>
        s""" (__ \\ "${field.fieldName}").format[${classDefinition(field.classPointer)}]"""
      case (true, true) =>
        s""" (__ \\ "${field.fieldName}").lazyFormat[${classDefinition(field.classPointer)}](Format.of[${classDefinition(field.classPointer)}])"""
      case (false, false) =>
        s""" (__ \\ "${field.fieldName}").formatNullable[${classDefinition(field.classPointer)}]"""
      case (false, true) =>
        s""" (__ \\ "${field.fieldName}").lazyFormatNullable[${classDefinition(field.classPointer)}](Format.of[${classDefinition(field.classPointer)}])"""
    }
  // format: on

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
    CaseClassGenerator(this).generate(generationAggr, toClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr =
    TraitGenerator(this).generate(generationAggr, toInterfaceDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr =
    EnumGenerator(this).generate(generationAggr, enumDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr =
    ClientClassGenerator(this).generate(generationAggr, clientClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr =
    ResourceClassGenerator(this).generate(generationAggr, resourceClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition): GenerationAggr =
    HeaderSegmentClassGenerator(this).generate(generationAggr, headerSegmentClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, unionClassDefinition: UnionClassDefinition): GenerationAggr =
    UnionClassGenerator(this).generate(generationAggr, unionClassDefinition)

  override val classFileExtension: String = "scala"

  override def toFilePath(classPointer: ClassPointer): Path = {
    classPointer match {
      case classReference: ClassReference =>
        val parts = classReference.safePackageParts :+ s"${classReference.name}.$classFileExtension"
        Paths.get("", parts: _*) // This results in a relative path both on Windows as on Linux/Mac
      case _ => sys.error(s"Cannot create a file path from a class pointer that is not a class reference!")
    }
  }

  val reservedKeywords =
    Set(
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

  def escapeScalaKeyword(someName: String, escape: String = "$"): String =
    reservedKeywords.foldLeft(someName) { (name, resWord) =>
      if (name == resWord) s"`$name`"
      else name
    }

}
