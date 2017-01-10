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

package io.atomicbits.scraml.generator.oldmodel

import io.atomicbits.scraml.generator.util.CleanNameUtil

/**
  * Created by peter on 11/09/15.
  *
  */
/**
  * Represents an abstract pointer to a class.
  */
sealed trait ClassPointer {

  def classDefinitionScala: String

  def classDefinitionJava: String

  def asTypedClassReference: TypedClassReference = {
    this match {
      case classReference: ClassReference           => TypedClassReference(classReference)
      case typedClassReference: TypedClassReference => typedClassReference
      case generic: TypeParameter =>
        sys.error(s"A generic object pointer cannot be transformed to a class pointer: $generic")
      case javaArray: JavaArray => sys.error("Typed version of a java array does not exist.")
    }
  }

  def packageName: String

  def fullyQualifiedName: String

  def safePackageParts: List[String]

  def canonicalNameScala: String

  def canonicalNameJava: String

  def isJavaArray: Boolean = false

}

/**
  * A type parameter points to a class via a parameter, while not knowing what the actual class is it points to.
  * E.g. "T" in List[T]
  */
case class TypeParameter(name: String) extends ClassPointer {

  def classDefinitionScala: String = name

  def classDefinitionJava: String = name

  override def fullyQualifiedName: String = sys.error("Cannot specify a fully qualified name of a type parameter.")

  override def packageName: String = sys.error("Cannot specify the package name of a type parameter.")

  override def safePackageParts: List[String] = sys.error("Cannot specify the package name of a type parameter.")

  override def canonicalNameJava: String = sys.error("Cannot specify the canonical name of a type parameter.")

  override def canonicalNameScala: String = sys.error("Cannot specify the canonical name of a type parameter.")
}

/**
  * A unique reference to a class. E.g. List[T].
  *
  * @param name           The name of the class.
  * @param packageParts   The package parts that comprise the full package name.
  * @param typeParameters The type parameters ths class reference holds.
  * @param predef         Indicates that the class is a predefined class that doesn't need to be imported to be used.
  * @param library        Indicates that the class is located in an existing library (and doesn't need to be generated).
  */
case class ClassReference(name: String,
                          packageParts: List[String]          = List.empty,
                          typeParameters: List[TypeParameter] = List.empty,
                          predef: Boolean                     = false,
                          library: Boolean                    = false)
    extends ClassPointer {

  def packageName: String = safePackageParts.mkString(".")

  def fullyQualifiedName: String = if (packageName.nonEmpty) s"$packageName.$name" else name

  // package parts need to be escaped in Java for Java keywords, Scala doesn't mind if you use keywords in a package name.
  // ToDo: only rename package parts that are keywords if we're generating Java.
  def safePackageParts: List[String] =
    packageParts.map(part => CleanNameUtil.escapeJavaKeyword(CleanNameUtil.cleanPackageName(part), "esc"))

  /**
    * The class definition as a string.
    * Todo: extract this Scala vs. Java code in the code generation
    *
    * E.g.:
    * "Boolean"
    * "User"
    * "List[T]"
    *
    */
  def classDefinitionScala: String =
    if (typeParameters.isEmpty) name
    else s"$name[${typeParameters.map(_.name).mkString(",")}]"

  /**
    * The class definition as a string.
    * Todo: extract this Scala vs. Java code in the code generation
    *
    * E.g.:
    * "Boolean"
    * "User"
    * "List<T>"
    *
    */
  def classDefinitionJava: String =
    if (typeParameters.isEmpty) name
    else s"$name<${typeParameters.map(_.name).mkString(",")}>"

  def canonicalNameScala: String = if (packageName.nonEmpty) s"$packageName.$classDefinitionScala" else classDefinitionScala

  def canonicalNameJava: String = if (packageName.nonEmpty) s"$packageName.$classDefinitionJava" else classDefinitionJava

}

/**
  * A class reference is like 'List[T]'
  * A typed class reference defines what the type parameters are by defining the actual type variables,
  *   e.g. T = String --> 'List[String]'
  */
case class TypedClassReference(classReference: ClassReference, typeVariables: Map[TypeParameter, TypedClassReference] = Map.empty)
    extends ClassPointer {

  /**
    * The class definition as a string.
    * Todo: extract this Scala vs. Java code in the code generation
    *
    * E.g.:
    * "Boolean"
    * "User"
    * "List[User]"
    * "List[List[User]]"
    */
  def classDefinitionScala: String =
    if (classReference.typeParameters.isEmpty) classReference.name
    else s"${classReference.name}[${classReference.typeParameters.map(typeVariables(_)).map(_.classDefinitionScala).mkString(",")}]"

  /**
    * The class definition as a string.
    * Todo: extract this Scala vs. Java code in the code generation
    *
    * E.g.:
    * "Boolean"
    * "User"
    * "List<User>"
    * "List<List<User>>"
    *
    */
  def classDefinitionJava: String =
    if (classReference.typeParameters.isEmpty) classReference.name
    else s"${classReference.name}<${classReference.typeParameters.map(typeVariables(_)).map(_.classDefinitionJava).mkString(",")}>"

  def packageName: String = classReference.packageName

  def fullyQualifiedName: String = classReference.fullyQualifiedName

  def safePackageParts: List[String] = classReference.safePackageParts

  def canonicalNameScala: String =
    if (classReference.typeParameters.isEmpty) classReference.fullyQualifiedName
    else s"${classReference.name}<${classReference.typeParameters.map(typeVariables(_)).map(_.canonicalNameScala).mkString(",")}>"

  def canonicalNameJava: String =
    if (classReference.typeParameters.isEmpty) classReference.fullyQualifiedName
    else
      s"${classReference.fullyQualifiedName}<${classReference.typeParameters.map(typeVariables(_)).map(_.canonicalNameJava).mkString(",")}>"

}

object StringClassReference {

  def apply(): ClassReference = ClassReference(name = "String", packageParts = List("java", "lang"), predef = true)

}

case object BooleanClassReference {

  def apply(required: Boolean)(implicit lang: Language): ClassReference = lang match {
    case Scala            => ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
    case Java if required => ClassReference(name = "boolean", packageParts = List("java", "lang"), predef = true)
    case Java             => ClassReference(name = "Boolean", packageParts = List("java", "lang"), predef = true)
  }

}

case object DoubleClassReference {

  def apply(required: Boolean)(implicit lang: Language): ClassReference = lang match {
    case Scala => ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
    // The case below goes wrong when the primitive ends up in a list like List<double> versus List<Double>.
    // case Java if required => ClassReference(name = "double", packageParts = List("java", "lang"), predef = true)
    case Java => ClassReference(name = "Double", packageParts = List("java", "lang"), predef = true)
  }
}

case object LongClassReference {

  def apply(required: Boolean)(implicit lang: Language): ClassReference = lang match {
    case Scala            => ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
    case Java if required => ClassReference(name = "long", packageParts = List("java", "lang"), predef = true)
    case Java             => ClassReference(name = "Long", packageParts = List("java", "lang"), predef = true)
  }

}

case object JsValueClassReference {

  def apply(): ClassReference = ClassReference(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)

}

case object JsObjectClassReference {

  def apply(): ClassReference = ClassReference(name = "JsObject", packageParts = List("play", "api", "libs", "json"), library = true)

}

case object FileClassReference {

  def apply(): ClassReference = ClassReference(name = "File", packageParts = List("java", "io"), library = true)

}

case object InputStreamClassReference {

  def apply(): ClassReference = ClassReference(name = "InputStream", packageParts = List("java", "io"), library = true)

}

case object JsonNodeClassReference {

  def apply(): ClassReference =
    ClassReference(
      name         = "JsonNode",
      packageParts = List("com", "fasterxml", "jackson", "databind"),
      library      = true
    )

}

object ByteClassReference {

  def apply()(implicit lang: Language): ClassReference = lang match {
    case Scala => ClassReference(name = "Byte", packageParts = List("scala"), predef        = true)
    case Java  => ClassReference(name = "Byte", packageParts = List("java", "lang"), predef = true)
  }

}

object BinaryDataClassReference {

  def apply()(implicit lang: Language): ClassReference = lang match {
    case Scala => ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "dsl"), library  = true)
    case Java  => ClassReference(name = "BinaryData", packageParts = List("io", "atomicbits", "scraml", "jdsl"), library = true)
  }

}

object ListClassReference {

  def apply(typeParamName: String)(implicit lang: Language): ClassReference = lang match {
    case Scala =>
      ClassReference(name = "List", typeParameters = List(TypeParameter(typeParamName)), predef = true)
    case Java =>
      ClassReference(name           = "List",
                     packageParts   = List("java", "util"),
                     typeParameters = List(TypeParameter(typeParamName)),
                     library        = true)
  }

  def typed(listType: ClassPointer)(implicit lang: Language): TypedClassReference =
    TypedClassReference(classReference = ListClassReference("T"),
                        typeVariables  = Map(TypeParameter("T") -> listType.asTypedClassReference))

}

object ArrayClassReference {

  def typed(arrayType: ClassReference)(implicit lang: Language): TypedClassReference = lang match {
    case Scala =>
      val typeParamName = "T"
      val classRef      = ClassReference(name = "Array", typeParameters = List(TypeParameter(typeParamName)), predef = true)
      TypedClassReference(classReference = classRef, typeVariables = Map(TypeParameter("T") -> arrayType.asTypedClassReference))
    case Java => throw new IllegalArgumentException("Java has no typed array representation.")
  }

}

case class JavaArray(name: String, packageParts: List[String] = List.empty, predef: Boolean = false) extends ClassPointer {

  def library: Boolean = true

  override def canonicalNameJava: String =
    if (packageName.nonEmpty) s"$packageName.$classDefinitionJava" else classDefinitionJava

  override def classDefinitionJava: String = s"$name[]"

  override def safePackageParts: List[String] =
    packageParts.map(part => CleanNameUtil.escapeJavaKeyword(CleanNameUtil.cleanPackageName(part), "esc"))

  override def classDefinitionScala: String = ???

  override def canonicalNameScala: String = ???

  override def fullyQualifiedName: String = if (packageName.nonEmpty) s"$packageName.$name" else name

  override def packageName: String = safePackageParts.mkString(".")

  override def isJavaArray: Boolean = true

}
