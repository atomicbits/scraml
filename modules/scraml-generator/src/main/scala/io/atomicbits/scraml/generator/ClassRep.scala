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

package io.atomicbits.scraml.generator

/**
 * Created by peter on 21/08/15. 
 */

/**
 *
 * @param name The name of this class.
 *             E.g. "ClassRep" for this class.
 * @param packageParts The package of this class, separated in its composing parts in ascending order.
 *                     E.g. List("io", "atomicbits", "scraml", "generator") for this class.
 * @param types The generic types of this class representation.
 * @param fields The public fields for this class rep (to become a scala case class or java pojo).
 * @param parentClass The class rep of the parent class of this class rep.
 * @param subClasses The class reps of the children of this class rep.
 * @param predef Indicates whether this class representation is a predefined type or not.
 *               Predefined types are: String, Boolean, Double, List, ... They don't need to be imported.
 * @param library Indicates whether this class representation is provided by a library or not.
 *                Library classes don't need to be generated (they already exist), but do need to be imported before you can use them.
 * @param content The source content of the class.
 */
case class ClassRep(name: String,
                    packageParts: List[String] = List.empty,
                    types: List[ClassRep] = List.empty,
                    fields: List[ClassAsFieldRep] = List.empty,
                    parentClass: Option[ClassRep] = None,
                    subClasses: List[ClassRep] = List.empty,
                    predef: Boolean = false,
                    library: Boolean = false,
                    content: Option[String] = None) {

  /**
   * The class definition as a string.
   *
   * E.g.:
   * "Boolean"
   * "User"
   * "List[User]"
   * "List[List[Address]]"
   *
   */
  val classDefinition: String =
    if (types.isEmpty) name
    else s"$name[${types.map(_.classDefinition).mkString(",")}]"


  val packageName: String = packageParts.mkString(".")

  val fullyQualifiedName: String = s"$packageName.$name"

}


case object StringClassRep extends ClassRep(name = "String", predef = true)

case object BooleanClassRep extends ClassRep(name = "Boolean", predef = true)

case object DoubleClassRep extends ClassRep(name = "Double", predef = true)

case object LongClassRep extends ClassRep(name = "Long", predef = true)

case object JsValueClassRep extends ClassRep(name = "JsValue", packageParts = List("play", "api", "libs", "json"), library = true)

object ListClassRep {

  def apply(listType: ClassRep): ClassRep = {
    ClassRep(name = "List", types = List(listType), predef = true)
  }

}

case class ClassAsFieldRep(fieldName: String, classRep: ClassRep, required: Boolean) {

  val fieldExpression: String =
    if (required) s"$fieldName: ${classRep.classDefinition}"
    else s"$fieldName: Option[${classRep.classDefinition}]"

}
