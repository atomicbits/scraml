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

import io.atomicbits.scraml.generator.lookup.SchemaLookup
import io.atomicbits.scraml.jsonschemaparser.AbsoluteId

import scala.annotation.tailrec

/**
 * Created by peter on 21/08/15. 
 */


trait ClassRep {

  def name: String

  def packageParts: List[String]

  def types: List[ClassRep]

  def fields: List[ClassAsFieldRep]

  def parentClass: Option[AbsoluteId]

  def subClasses: List[AbsoluteId]

  def predef: Boolean

  def library: Boolean

  def content: Option[String]

  def jsonTypeInfo: Option[JsonTypeInfo]

  def withFields(fields: List[ClassAsFieldRep]): ClassRep

  def withParent(parentId: AbsoluteId): ClassRep

  def withChildren(childIds: List[AbsoluteId]): ClassRep

  def withContent(content: String): ClassRep

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep

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
  def classDefinition: String =
    if (types.isEmpty) name
    else s"$name[${types.map(_.classDefinition).mkString(",")}]"


  def packageName: String = packageParts.mkString(".")

  def fullyQualifiedName: String = if (packageName.nonEmpty) s"$packageName.$name" else name

  def isInHierarchy: Boolean = parentClass.isDefined || subClasses.nonEmpty

  /**
   * Gives the top level parent of the hierarchy this class rep takes part in if any. If this class rep is the top level class,
   * it will be returned as the result (as opposed to the method topLevelParent).
   */
  def hierarchyParent(schemaLookup: SchemaLookup): Option[ClassRep] = {
    if (parentClass.isEmpty && subClasses.nonEmpty) Some(this)
    else topLevelParent(schemaLookup)
  }

  /**
   * Gives the top level parent of this class rep. A top level parent class itself has no parent and thus no top level parent.
   */
  def topLevelParent(schemaLookup: SchemaLookup): Option[ClassRep] = {

    @tailrec
    def findTopLevelParent(parentId: AbsoluteId): ClassRep = {
      val parentClass = schemaLookup.classReps(parentId)
      parentClass.parentClass match {
        case Some(prntId) => findTopLevelParent(prntId)
        case None         => parentClass
      }
    }

    parentClass.map(findTopLevelParent)

  }

}

trait LibraryClassRep extends ClassRep {

  def types: List[ClassRep] = List.empty

  def fields: List[ClassAsFieldRep] = List.empty

  def parentClass: Option[AbsoluteId] = None

  def subClasses: List[AbsoluteId] = List.empty

  def predef: Boolean = false

  def library: Boolean = true

  def content: Option[String] = None

  def jsonTypeInfo: Option[JsonTypeInfo] = None

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = sys.error("We shouldn't set the fields of a library class rep.")

  def withContent(content: String): ClassRep = sys.error("We shouldn't set the content of a library class rep.")

  def withParent(parentId: AbsoluteId): ClassRep = sys.error("We shouldn't set the parent of a library class rep.")

  def withChildren(childIds: List[AbsoluteId]): ClassRep = sys.error("We shouldn't set the children of a library class rep.")

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = sys.error("We shouldn't set JSON type info on a library class rep.")

}


trait PredefinedClassRep extends ClassRep {

  def packageParts: List[String] = List.empty

  def types: List[ClassRep] = List.empty

  def fields: List[ClassAsFieldRep] = List.empty

  def parentClass: Option[AbsoluteId] = None

  def subClasses: List[AbsoluteId] = List.empty

  def predef: Boolean = true

  def library: Boolean = false

  def content: Option[String] = None

  def jsonTypeInfo: Option[JsonTypeInfo] = None

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = sys.error("We shouldn't set the fields of a predefined class rep.")

  def withContent(content: String): ClassRep = sys.error("We shouldn't set the content of a predefined class rep.")

  def withParent(parentId: AbsoluteId): ClassRep = sys.error("We shouldn't set the parent of a predefined class rep.")

  def withChildren(childIds: List[AbsoluteId]): ClassRep = sys.error("We shouldn't set the children of a predefined class rep.")

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep =
    sys.error("We shouldn't set JSON type info on a predefined class rep.")

}


case object StringClassRep extends PredefinedClassRep {

  val name = "String"

}

case object BooleanClassRep extends PredefinedClassRep {

  val name = "Boolean"

}

case object DoubleClassRep extends PredefinedClassRep {

  val name = "Double"

}

case object LongClassRep extends PredefinedClassRep {

  val name = "Long"

}

case object JsValueClassRep extends LibraryClassRep {

  val name = "JsValue"

  val packageParts = List("play", "api", "libs", "json")

}


object ListClassRep {

  def apply(listType: ClassRep): ClassRep = {
    ClassRep(name = "List", types = List(listType), predef = true)
  }

}

case class CustomClassRep(name: String,
                          packageParts: List[String] = List.empty,
                          types: List[ClassRep] = List.empty,
                          fields: List[ClassAsFieldRep] = List.empty,
                          parentClass: Option[AbsoluteId] = None,
                          subClasses: List[AbsoluteId] = List.empty,
                          predef: Boolean = false,
                          library: Boolean = false,
                          content: Option[String] = None,
                          jsonTypeInfo: Option[JsonTypeInfo] = None) extends ClassRep {

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = copy(fields = fields)

  def withContent(content: String): ClassRep = copy(content = Some(content))

  def withParent(parentId: AbsoluteId): ClassRep = copy(parentClass = Some(parentId))

  def withChildren(childIds: List[AbsoluteId]): ClassRep = copy(subClasses = childIds)

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = copy(jsonTypeInfo = Some(jsonTypeInfo))

}


case class ClassAsFieldRep(fieldName: String, classRep: ClassRep, required: Boolean) {

  def fieldExpression: String =
    if (required) s"$fieldName: ${classRep.classDefinition}"
    else s"$fieldName: Option[${classRep.classDefinition}] = None"

}

case class JsonTypeInfo(discriminator: String, discriminatorValue: Option[String])

object ClassRep {

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
  def apply(name: String,
            packageParts: List[String] = List.empty,
            types: List[ClassRep] = List.empty,
            fields: List[ClassAsFieldRep] = List.empty,
            parentClass: Option[AbsoluteId] = None,
            subClasses: List[AbsoluteId] = List.empty,
            predef: Boolean = false,
            library: Boolean = false,
            content: Option[String] = None,
            jsonTypeInfo: Option[JsonTypeInfo] = None): ClassRep = {

    name match {
      case "String"  => StringClassRep
      case "Boolean" => BooleanClassRep
      case "Double"  => DoubleClassRep
      case "Long"    => LongClassRep
      case "JsValue" => JsValueClassRep
      case _         => CustomClassRep(name, packageParts, types, fields, parentClass, subClasses, predef, library, content, jsonTypeInfo)
    }

  }

}