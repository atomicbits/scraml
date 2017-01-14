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

/**
  * Created by peter on 10/01/17.
  */
object ScalaPlay extends Platform with CleanNameTools {

  val stringClassReference = ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)

  def longClassReference(primitive: Boolean = false) = ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)

  def doubleClassReference(primitive: Boolean = false): ClassReference =
    ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)

  def booleanClassReference(primitive: Boolean = false): ClassReference =
    ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)

  def arrayClassReference(arrayType: ClassReference): ClassPointer = {
    val typeParameter = TypeParameter("T")
    ClassReference(name = "Array", typeParameters = List(typeParameter), typeParamValues = Map(typeParameter -> arrayType), predef = true)
  }

  def listClassReference(typeParamName: String): ClassReference =
    ClassReference(name = "List", typeParameters = List(TypeParameter(typeParamName)), predef = true)

  val byteClassReference: ClassReference = ClassReference(name = "Byte", packageParts = List("scala"), predef = true)

  val binaryDataClassReference: ClassReference =
    ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "dsl"), library = true)

  val fileClassReference: ClassReference = ClassReference(name = "File", packageParts = List("java", "io"), library = true)

  val inputStreamClassReference: ClassReference = ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)

  val jsObjectClassReference: ClassReference =
    ClassReference(name = "JsObject", packageParts = List("play", "api", "libs", "json"), library = true)

  val jsValueClassReference: ClassReference =
    ClassReference(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)

  override def classDefinition(classPointer: ClassPointer): String =
    classPointer match {
      case classReference: ClassReference =>
        if (classReference.typeParameters.isEmpty) classReference.name
        else s"${classReference.name}[${classReference.typeParameters.map(_.name).mkString(",")}]"
      case arrayClassReference: ArrayClassReference => sys.error(s"Scala has no array class representation.")
      case typeParameter: TypeParameter             => sys.error(s"A type parameter has no class definition.")
    }

  override def className(classPointer: ClassPointer): String =
    classPointer match {
      case classReference: ClassReference           => classReference.name
      case arrayClassReference: ArrayClassReference => sys.error(s"Scala has no array class representation.")
      case typeParameter: TypeParameter             => sys.error(s"A type parameter has no class definition.")
    }

  override def packageName(classPointer: ClassPointer): String = safePackageParts(classPointer).mkString(".")

  override def fullyQualifiedName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ className(classPointer)
    parts.mkString(".")
  }

  override def safePackageParts(classPointer: ClassPointer): List[String] =
    classPointer match {
      case classReference: ClassReference           => classReference.packageParts
      case arrayClassReference: ArrayClassReference => sys.error(s"Scala has no array class representation.")
      case typeParameter: TypeParameter             => sys.error(s"A type parameter has no package parts.")
    }

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

  override def toSourceFile(toClassDefinition: TransferObjectClassDefinition): SourceFile =
    CaseClassGenerator.generate(toClassDefinition)

  override def toSourceFile(toInterfaceDefinition: TransferObjectInterfaceDefinition): SourceFile =
    TraitGenerator.generate(toInterfaceDefinition)

  override def toSourceFile(enumDefinition: EnumDefinition): SourceFile =
    EnumGenerator.generate(enumDefinition)

  override def toSourceFile(clientClassDefinition: ClientClassDefinition): SourceFile =
    ClientClassGenerator.generate(clientClassDefinition)

  override def toSourceFile(resourceClassDefinition: ResourceClassDefinition): SourceFile =
    ResourceClassGenerator.generate(resourceClassDefinition)

  override def toSourceFile(unionClassDefinition: UnionClassDefinition): SourceFile =
    UnionClassGenerator.generate(unionClassDefinition)

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
