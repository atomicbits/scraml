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

package io.atomicbits.scraml.generator.model

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
      case generic: GenericClassPointer            =>
        sys.error(s"A generic object pointer cannot be transformed to a class pointer: $generic")
    }
  }

  def packageName: String

  def fullyQualifiedName: String

  def canonicalNameScala: String

  def canonicalNameJava: String

}


/**
 * A generic class pointer points to a class via a variable, while not knowing what the actual class is it points to.
 * E.g. "T" in List[T]
 */
case class GenericClassPointer(typeVariable: String) extends ClassPointer {

  def classDefinitionScala: String = typeVariable

  def classDefinitionJava: String = typeVariable

  override def fullyQualifiedName: String = sys.error("Cannot specify a fully qualified name of a generic class pointer.")

  override def packageName: String = sys.error("Cannot specify the package name of a generic class pointer.")

  override def canonicalNameJava: String = sys.error("Cannot specify the canonical name of a generic class pointer.")

  override def canonicalNameScala: String = sys.error("Cannot specify the canonical name of a generic class pointer.")
}


/**
 * A unique reference to a class. E.g. List[T].
 */
case class ClassReference(name: String,
                          packageParts: List[String] = List.empty,
                          typeVariables: List[String] = List.empty,
                          predef: Boolean = false,
                          library: Boolean = false) extends ClassPointer {

  def packageName: String = packageParts.mkString(".")

  def fullyQualifiedName: String = if (packageName.nonEmpty) s"$packageName.$name" else name

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
    if (typeVariables.isEmpty) name
    else s"$name[${typeVariables.mkString(",")}]"


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
    if (typeVariables.isEmpty) name
    else s"$name<${typeVariables.mkString(",")}>"


  def canonicalNameScala: String  = if (packageName.nonEmpty) s"$packageName.$classDefinitionScala" else classDefinitionScala


  def canonicalNameJava: String  = if (packageName.nonEmpty) s"$packageName.$classDefinitionJava" else classDefinitionJava

}


/**
 * A class reference is like 'List[T]'
 * A typed class reference defines what the type variables are, e.g. 'List[String]'
 */
case class TypedClassReference(classReference: ClassReference,
                               types: Map[String, TypedClassReference] = Map.empty) extends ClassPointer {

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
    if (classReference.typeVariables.isEmpty) classReference.name
    else s"${classReference.name}[${classReference.typeVariables.map(types(_)).map(_.classDefinitionScala).mkString(",")}]"


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
    if (classReference.typeVariables.isEmpty) classReference.name
    else s"${classReference.name}<${classReference.typeVariables.map(types(_)).map(_.classDefinitionJava).mkString(",")}>"


  def packageName: String = classReference.packageName


  def fullyQualifiedName: String = classReference.fullyQualifiedName


  def canonicalNameScala: String =
    if (classReference.typeVariables.isEmpty) classReference.fullyQualifiedName
    else s"${classReference.name}<${classReference.typeVariables.map(types(_)).map(_.canonicalNameScala).mkString(",")}>"


  def canonicalNameJava: String =
    if (classReference.typeVariables.isEmpty) classReference.fullyQualifiedName
    else s"${classReference.name}<${classReference.typeVariables.map(types(_)).map(_.canonicalNameJava).mkString(",")}>"

}
