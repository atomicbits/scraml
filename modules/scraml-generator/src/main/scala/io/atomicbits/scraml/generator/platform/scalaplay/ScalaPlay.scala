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

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, CanonicalType }
import Platform._
import io.atomicbits.scraml.generator.codegen.GenerationAggr

/**
  * Created by peter on 10/01/17.
  */
object ScalaPlay extends Platform with CleanNameTools {

  implicit val platform = ScalaPlay

  override def classPointerToNativeClassReference(classPointer: ClassPointer): ClassReference = {
    classPointer match {
      case classReference: ClassReference => classReference
      case ArrayClassReference(arrayType) =>
        val typeParameter = TypeParameter("T")
        ClassReference(name            = "Array",
                       typeParameters  = List(typeParameter),
                       typeParamValues = Map(typeParameter -> arrayType),
                       predef          = true)
      case StringClassReference =>
        ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)
      case ByteClassReference =>
        ClassReference(name = "Byte", packageParts = List("scala"), predef = true)
      case BinaryDataClassReference =>
        ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "dsl"), library = true)
      case FileClassReference =>
        ClassReference(name = "File", packageParts = List("java", "io"), library = true)
      case InputStreamClassReference =>
        ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)
      case JsObjectClassReference =>
        ClassReference(name = "JsObject", packageParts = List("play", "api", "libs", "json"), library = true)
      case JsValueClassReference =>
        ClassReference(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)
      case BodyPartClassReference =>
        ClassReference(name = "BodyPart", packageParts = List("io", "atomicbits", "scraml", "dsl"), library = true)
      case LongClassReference(primitive) =>
        ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
      case DoubleClassReference(primitive) =>
        ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
      case BooleanClassReference(primitive) =>
        ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
      case ListClassReference(typeParamValue) =>
        val typeParameter   = TypeParameter("T")
        val typeParamValues = Map(typeParameter -> typeParamValue)
        ClassReference(name = "List", typeParameters = List(typeParameter), typeParamValues = typeParamValues, predef = true)
      case typeParameter: TypeParameter =>
        ClassReference(name = typeParameter.name, predef = true, isTypeParameter = true)
    }
  }

  override def interfaceReference(classReference: ClassReference): ClassReference =
    ClassReference(
      name         = s"${classReference.name}Trait",
      packageParts = classReference.packageParts
    )

  override def classDefinition(classPointer: ClassPointer): String = {
    val classReference = classPointer.native

    if (classReference.typeParameters.isEmpty) {
      classReference.name
    } else {
      val typeParametersOrValues = classReference.typeParameters.map { typeParam =>
        classReference.typeParamValues.get(typeParam).map(classPointer => classPointer.native.classDefinition).getOrElse(typeParam.name)
      }
      s"${classReference.name}[${typeParametersOrValues.mkString(",")}]"
    }
  }

  override def className(classPointer: ClassPointer): String = classPointer.native.name

  override def packageName(classPointer: ClassPointer): String = safePackageParts(classPointer).mkString(".")

  override def fullyQualifiedName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ className(classPointer)
    parts.mkString(".")
  }

  override def safePackageParts(classPointer: ClassPointer): List[String] = classPointer.native.packageParts

  override def canonicalName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ classDefinition(classPointer)
    parts.mkString(".")
  }

  override def safeFieldName(field: Field): String = {
    val cleanName = cleanFieldName(field.fieldName)
    escapeScalaKeyword(cleanName)
  }

  override def fieldExpression(field: Field): String = {
    if (field.required) s"${safeFieldName(field)}: ${classDefinition(field.classPointer)}"
    else s"${safeFieldName(field)}: Option[${classDefinition(field.classPointer)}] = None"
  }

  def fieldFormatUnlift(field: Field): String =
    if (field.required)
      s""" (__ \\ "${field.fieldName}").format[${classDefinition(field.classPointer)}]"""
    else
      s""" (__ \\ "$field.fieldName").formatNullable[${classDefinition(field.classPointer)}]"""

  override def importStatements(imports: Set[ClassPointer]): Set[String] = {
    imports.map(_.native).collect {
      case classReference if !classReference.predef => s"import ${classReference.fullyQualifiedName}"
    }
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

  override def classFileExtension: String = ".scala"

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
      if (name == resWord) s"$name$$"
      else name
    }

  }

}
