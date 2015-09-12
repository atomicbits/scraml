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

import io.atomicbits.scraml.generator.model.ClassRep.ClassMap

import scala.annotation.tailrec

/**
 * Created by peter on 21/08/15. 
 */


trait ClassRep {

  def name: String

  def packageParts: List[String]

  def types: List[ClassRep]

  def fields: List[ClassAsFieldRep]

  def parentClass: Option[ClassReference]

  def subClasses: List[ClassReference]

  def predef: Boolean

  def library: Boolean

  def content: Option[String]

  def jsonTypeInfo: Option[JsonTypeInfo]

  def withFields(fields: List[ClassAsFieldRep]): ClassRep

  def withParent(parentId: ClassReference): ClassRep

  def withChildren(childIds: List[ClassReference]): ClassRep

  def withContent(content: String): ClassRep

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep

  def classRef = ClassReference(name, packageParts)

  /**
   * The class definition as a string.
   * Todo: extract this Scala vs. Java code in the code generation
   *
   * E.g.:
   * "Boolean"
   * "User"
   * "List[User]"
   * "List[List[Address]]"
   *
   */
  def classDefinitionScala: String =
    if (types.isEmpty) name
    else s"$name[${types.map(_.classDefinitionScala).mkString(",")}]"


  /**
   * The class definition as a string.
   * Todo: extract this Scala vs. Java code in the code generation
   *
   * E.g.:
   * "Boolean"
   * "User"
   * "List<User>"
   * "List<List<Address>>"
   *
   */
  def classDefinitionJava: String =
    if (types.isEmpty) name
    else s"$name<${types.map(_.classDefinitionScala).mkString(",")}>"


  def packageName: String = packageParts.mkString(".")

  def fullyQualifiedName: String = if (packageName.nonEmpty) s"$packageName.$name" else name

  def isInHierarchy: Boolean = parentClass.isDefined || subClasses.nonEmpty

  /**
   * Gives the top level parent of the hierarchy this class rep takes part in if any. If this class rep is the top level class,
   * it will be returned as the result (as opposed to the method topLevelParent).
   */
  def hierarchyParent(classMap: ClassMap): Option[ClassRep] = {
    if (parentClass.isEmpty && subClasses.nonEmpty) Some(this)
    else topLevelParent(classMap)
  }

  /**
   * Gives the top level parent of this class rep. A top level parent class itself has no parent and thus no top level parent.
   */
  def topLevelParent(classMap: ClassMap): Option[ClassRep] = {

    @tailrec
    def findTopLevelParent(parentId: ClassReference): ClassRep = {
      val parentClass = classMap(parentId)
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

  def parentClass: Option[ClassReference] = None

  def subClasses: List[ClassReference] = List.empty

  def predef: Boolean = false

  def library: Boolean = true

  def content: Option[String] = None

  def jsonTypeInfo: Option[JsonTypeInfo] = None

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = sys.error("We shouldn't set the fields of a library class rep.")

  def withContent(content: String): ClassRep = sys.error("We shouldn't set the content of a library class rep.")

  def withParent(parentId: ClassReference): ClassRep = sys.error("We shouldn't set the parent of a library class rep.")

  def withChildren(childIds: List[ClassReference]): ClassRep = sys.error("We shouldn't set the children of a library class rep.")

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = sys.error("We shouldn't set JSON type info on a library class rep.")

}


trait PredefinedClassRep extends ClassRep {

  def packageParts: List[String] = List.empty

  def types: List[ClassRep] = List.empty

  def fields: List[ClassAsFieldRep] = List.empty

  def parentClass: Option[ClassReference] = None

  def subClasses: List[ClassReference] = List.empty

  def predef: Boolean = true

  def library: Boolean = false

  def content: Option[String] = None

  def jsonTypeInfo: Option[JsonTypeInfo] = None

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = sys.error("We shouldn't set the fields of a predefined class rep.")

  def withContent(content: String): ClassRep = sys.error("We shouldn't set the content of a predefined class rep.")

  def withParent(parentId: ClassReference): ClassRep = sys.error("We shouldn't set the parent of a predefined class rep.")

  def withChildren(childIds: List[ClassReference]): ClassRep = sys.error("We shouldn't set the children of a predefined class rep.")

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


case class EnumValuesClassRep(name: String,
                              values: List[String] = List.empty,
                              packageParts: List[String] = List.empty,
                              types: List[ClassRep] = List.empty,
                              parentClass: Option[ClassReference] = None,
                              subClasses: List[ClassReference] = List.empty,
                              predef: Boolean = false,
                              library: Boolean = false,
                              content: Option[String] = None,
                              jsonTypeInfo: Option[JsonTypeInfo] = None) extends ClassRep {

  val fields: List[ClassAsFieldRep] = Nil

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = sys.error("An EnumValueclassRep has no fields")

  def withContent(content: String): ClassRep = copy(content = Some(content))

  def withParent(parentId: ClassReference): ClassRep = copy(parentClass = Some(parentId))

  def withChildren(childIds: List[ClassReference]): ClassRep = copy(subClasses = childIds)

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = copy(jsonTypeInfo = Some(jsonTypeInfo))

}

object EnumValuesClassRep {

  def apply(classRep: ClassRep, values: List[String]): EnumValuesClassRep =
    EnumValuesClassRep(name = classRep.name, packageParts = classRep.packageParts, values = values)

}


object ListClassRep {

  def apply(listType: ClassRep): ClassRep = {
    ClassRep(classReference = ClassReference(name = "List"), types = List(listType), predef = true)
  }

}

case class CustomClassRep(name: String,
                          packageParts: List[String] = List.empty,
                          types: List[ClassRep] = List.empty,
                          fields: List[ClassAsFieldRep] = List.empty,
                          parentClass: Option[ClassReference] = None,
                          subClasses: List[ClassReference] = List.empty,
                          predef: Boolean = false,
                          library: Boolean = false,
                          content: Option[String] = None,
                          jsonTypeInfo: Option[JsonTypeInfo] = None) extends ClassRep {

  def withFields(fields: List[ClassAsFieldRep]): ClassRep = copy(fields = fields)

  def withContent(content: String): ClassRep = copy(content = Some(content))

  def withParent(parentId: ClassReference): ClassRep = copy(parentClass = Some(parentId))

  def withChildren(childIds: List[ClassReference]): ClassRep = copy(subClasses = childIds)

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = copy(jsonTypeInfo = Some(jsonTypeInfo))

}


case class ClassAsFieldRep(fieldName: String, classRep: ClassRep, required: Boolean) {

  def fieldExpressionScala: String =
    if (required) s"$fieldName: ${classRep.classDefinitionScala}"
    else s"$fieldName: Option[${classRep.classDefinitionScala}] = None"

  def fieldExpressionJava: String = s"${classRep.classDefinitionJava} $fieldName"

}


case class JsonTypeInfo(discriminator: String, discriminatorValue: Option[String])

object ClassRep {

  type ClassMap = Map[ClassReference, ClassRep]

  /**
   *
   * @param classReference The class reference for the class representation.
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
  def apply(classReference: ClassReference,
            types: List[ClassRep] = List.empty,
            fields: List[ClassAsFieldRep] = List.empty,
            parentClass: Option[ClassReference] = None,
            subClasses: List[ClassReference] = List.empty,
            predef: Boolean = false,
            library: Boolean = false,
            content: Option[String] = None,
            jsonTypeInfo: Option[JsonTypeInfo] = None): ClassRep = {

    classReference.name match {
      case "String"  => StringClassRep
      case "Boolean" => BooleanClassRep
      case "Double"  => DoubleClassRep
      case "Long"    => LongClassRep
      case "JsValue" => JsValueClassRep
      case _         => CustomClassRep(
        classReference.name,
        classReference.packageParts,
        types,
        fields,
        parentClass,
        subClasses,
        predef,
        library,
        content,
        jsonTypeInfo
      )
    }

  }

}