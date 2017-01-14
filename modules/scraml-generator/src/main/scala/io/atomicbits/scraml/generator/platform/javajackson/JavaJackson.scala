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

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.typemodel._

/**
  * Created by peter on 10/01/17.
  */
object JavaJackson extends Platform with CleanNameTools {

  val stringClassReference: ClassReference = ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)

  def longClassReference(primitive: Boolean = false): ClassReference =
    if (primitive) {
      ClassReference(name = "long", packageParts = List("java", "lang"), predef = true)
    } else {
      ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
    }

  def doubleClassReference(primitive: Boolean = false): ClassReference =
    if (primitive) {
      ClassReference(name = "double", packageParts = List("java", "lang"), predef = true)
    } else {
      ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
    }

  def booleanClassReference(primitive: Boolean = false): ClassReference =
    if (primitive) {
      ClassReference(name = "boolean", packageParts = List("java", "lang"), predef = true)
    } else {
      ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
    }

  def arrayClassReference(arrayType: ClassReference): ClassPointer = ArrayClassReference(arrayType)

  def listClassReference(typeParamName: String): ClassReference =
    ClassReference(name = "List", packageParts = List("java", "util"), typeParameters = List(TypeParameter(typeParamName)), library = true)

  val byteClassReference: ClassReference = ClassReference(name = "Byte", packageParts = List("java", "lang"), predef = true)

  val binaryDataClassReference: ClassReference =
    ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "jdsl"), library = true)

  val fileClassReference: ClassReference = ClassReference(name = "File", packageParts = List("java", "io"), library = true)

  val inputStreamClassReference: ClassReference = ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)

  val jsValueClassReference: ClassReference =
    ClassReference(name = "JsonNode", packageParts = List("com", "fasterxml", "jackson", "databind"), library = true)

  val jsObjectClassReference: ClassReference = jsValueClassReference

  override def classDefinition(classPointer: ClassPointer): String =
    classPointer match {
      case classReference: ClassReference =>
        if (classReference.typeParameters.isEmpty) classReference.name
        else s"${classReference.name}<${classReference.typeParameters.map(_.name).mkString(",")}>"
      case arrayClassReference: ArrayClassReference => sys.error(s"Scala has no array class representation.")
      case typeParameter: TypeParameter             => sys.error(s"A type parameter has no class definition.")
    }

  def className(classPointer: ClassPointer): String =
    classPointer match {
      case classReference: ClassReference           => classReference.name
      case arrayClassReference: ArrayClassReference => sys.error(s"There is no class name for an array class reference")
      case typeParameter: TypeParameter             => sys.error(s"A type parameter has no class name.")
    }

  override def packageName(classPointer: ClassPointer): String = safePackageParts(classPointer).mkString(".")

  override def fullyQualifiedName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ className(classPointer)
    parts.mkString(".")
  }

  override def safePackageParts(classPointer: ClassPointer): List[String] =
    classPointer match {
      case classReference: ClassReference =>
        classReference.packageParts.map(part => escapeJavaKeyword(cleanPackageName(part), "esc"))
      case arrayClassReference: ArrayClassReference =>
        arrayClassReference.refers.packageParts.map(part => escapeJavaKeyword(cleanPackageName(part), "esc"))
      case typeParameter: TypeParameter => sys.error(s"A type parameter has no package parts.")
    }

  override def canonicalName(classPointer: ClassPointer): String = {
    val parts: List[String] = safePackageParts(classPointer) :+ classDefinition(classPointer)
    parts.mkString(".")
  }

  override def safeFieldName(field: Field): String = {
    val cleanName = cleanFieldName(field.fieldName)
    escapeJavaKeyword(cleanName)
  }

  override def fieldExpression(field: Field): String = {
    s"${classDefinition(field.classPointer)} ${safeFieldName(field)}"
  }

  override def toSourceFile(classDefinition: TransferObjectClassDefinition): SourceFile = ???

  override def toSourceFile(toInterfaceDefinition: TransferObjectInterfaceDefinition): SourceFile = ???

  override def toSourceFile(enumDefinition: EnumDefinition): SourceFile = ???

  override def toSourceFile(clientClassDefinition: ClientClassDefinition): SourceFile = ???

  override def toSourceFile(resourceClassDefinition: ResourceClassDefinition): SourceFile = ???

  override def toSourceFile(unionClassDefinition: UnionClassDefinition): SourceFile = ???

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
