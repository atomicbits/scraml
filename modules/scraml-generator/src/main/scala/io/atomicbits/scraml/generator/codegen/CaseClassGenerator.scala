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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.model.ClassRep.ClassMap
import io.atomicbits.scraml.generator.model._


/**
 * Created by peter on 4/06/15, Atomic BITS (http://atomicbits.io).
 *
 * JSON schema and referencing:
 * http://json-schema.org/latest/json-schema-core.html
 * http://tools.ietf.org/html/draft-zyp-json-schema-03
 * http://spacetelescope.github.io/understanding-json-schema/structuring.html
 * http://forums.raml.org/t/how-do-you-reference-another-schema-from-a-schema/485
 *
 */
object CaseClassGenerator extends DtoSupport {


  def generateCaseClasses(classMap: ClassMap): List[ClassRep] = {

    // Expand all canonical names into their case class definitions.

    val (classRepsInHierarcy, classRepsStandalone) = classMap.values.toList.partition(_.isInHierarchy)

    val classHierarchies = classRepsInHierarcy.groupBy(_.hierarchyParent(classMap))
      .collect { case (Some(classRep), reps) => (classRep, reps) }

    classHierarchies.values.toList.flatMap(generateHierarchicalClassSource(_, classMap)) :::
      classRepsStandalone.map(generateNonHierarchicalClassSource(_, classMap))
  }


  def generateNonHierarchicalClassSource(classRep: ClassRep, classMap: ClassMap): ClassRep = {

    println(s"Generating case class for: ${classRep.classDefinitionScala}")


    classRep match {
      case e: EnumValuesClassRep => generateEnumClassRep(e)
      case _                     => generateNonEnumClassRep(classRep)
    }
  }


  private def generateEnumClassRep(classRep: EnumValuesClassRep): ClassRep = {
    val imports: Set[String] = collectImports(classRep)

    def enumValue(value: String): String = {
      s"""
         case object $value extends ${classRep.name} {
           val name = "$value"
         }
      """
    }

    def generateEnumCompanionObject: String = {

      val name = classRep.name
      s"""
        object $name {

          ${classRep.values.map(enumValue).mkString("\n")}

          val byName = Map(
            ${classRep.values.map { v => s"$v.name -> $v" }.mkString(",")}
          )

          implicit val ${name}Format = new Format[$name] {

            override def reads(json: JsValue): JsResult[$name] = {
              json.validate[String].map($name.byName(_))
            }

            override def writes(o: $name): JsValue = {
              JsString(o.name)
            }
          }
        }
       """
    }

    val source =
      s"""
        package ${classRep.packageName}

        import play.api.libs.json.{Format, Json, JsResult, JsValue, JsString}

        ${imports.mkString("\n")}

        sealed trait ${classRep.name} {
          def name:String
        }

        $generateEnumCompanionObject
     """

    classRep.withContent(content = source)
  }


  private def generateNonEnumClassRep(classRep: ClassRep): ClassRep = {

    val imports: Set[String] = collectImports(classRep)

    val source =
      s"""
        package ${classRep.packageName}

        import play.api.libs.json._

        ${imports.mkString("\n")}

        ${generateCaseClassWithCompanion(classRep)}
     """

    classRep.withContent(content = source)
  }


  private def generateCaseClassWithCompanion(classRep: ClassRep,
                                             parentClassRep: Option[ClassRep] = None,
                                             skipFieldName: Option[String] = None): String = {

    val fieldsWithParentFields =
      parentClassRep map { parentClass =>
        classRep.fields ++ parentClass.fields
      } getOrElse classRep.fields

    val selectedFields =
      skipFieldName map { skipField =>
        fieldsWithParentFields.filterNot(_.fieldName == skipField)
      } getOrElse fieldsWithParentFields

    val sortedFields = selectedFields.sortBy(field => (!field.required, field.fieldName))
    val fieldExpressions = sortedFields.map(_.fieldExpressionScala)

    val extendsClass = parentClassRep.map(parentClassRep => s"extends ${parentClassRep.classDefinitionScala}").getOrElse("")

    val formatUnLiftFields = sortedFields.map(_.fieldFormatUnliftScala)

    // ToDo: see how we can cleanup these nested if-statements below!
    val formatter = {
      if (classRep.classRef.typeVariables.nonEmpty) {
        // "Complex version for typed variables"
        /**
         * This is the only way we know that formats typed variables, but it has problems with recursive types,
         * (see https://www.playframework.com/documentation/2.4.x/ScalaJsonCombinators#Recursive-Types).
         */
        val typeVariables = classRep.classRef.typeVariables.map(typeVar => s"$typeVar: Format")

        if (formatUnLiftFields.size == 1) {
          s"""
            import play.api.libs.functional.syntax._

            implicit def jsonFormatter[${typeVariables.mkString(",")}]: Format[${classRep.classDefinitionScala}] =
              ${formatUnLiftFields.head}.inmap(${classRep.name}.apply, unlift(${classRep.name}.unapply))
           """
        } else {
          s"""
            import play.api.libs.functional.syntax._

            implicit def jsonFormatter[${typeVariables.mkString(",")}]: Format[${classRep.classDefinitionScala}] =
              ( ${formatUnLiftFields.mkString("~\n")}
              )(${classRep.name}.apply, unlift(${classRep.name}.unapply))
           """
        }
      }
      else {

        val anyFieldRenamed = sortedFields.exists(field => field.fieldName != field.safeFieldNameScala)

        if (anyFieldRenamed) {
          // "Complex version"
          if (formatUnLiftFields.size == 1) {
            s"""
           import play.api.libs.functional.syntax._

           implicit def jsonFormatter: Format[${classRep.classDefinitionScala}] =
             ${formatUnLiftFields.head}.inmap(${classRep.name}.apply, unlift(${classRep.name}.unapply))
         """
          } else {
            s"""
           import play.api.libs.functional.syntax._

           implicit def jsonFormatter: Format[${classRep.classDefinitionScala}] =
             ( ${formatUnLiftFields.mkString("~\n")}
             )(${classRep.name}.apply, unlift(${classRep.name}.unapply))
         """
          }
        } else {
          // "Easy version"
          /**
           * The reason why we like to use the easy macro version below is that it resolves issues like the recursive
           * type problem that the elaborate "Complex version" has
           * (see https://www.playframework.com/documentation/2.4.x/ScalaJsonCombinators#Recursive-Types)
           * Types like the one below cannot be formatted with the "Complex version":
           *
           * > case class Tree(value: String, children: List[Tree])
           *
           * To format it with the "Complex version" has to be done as follows:
           *
           * > case class Tree(value: String, children: List[Tree])
           * > object Tree {
           * >   import play.api.libs.functional.syntax._
           * >   implicit def jsonFormatter: Format[Tree] = // Json.format[Tree]
           * >     ((__ \ "value").format[String] ~
           * >       (__ \ "children").lazyFormat[List[Tree]](Reads.list[Tree](jsonFormatter), Writes.list[Tree](jsonFormatter)))(Tree.apply, unlift(Tree.unapply))
           * > }
           *
           * To format it with the "Easy version" is simply:
           *
           * > implicit val jsonFormatter: Format[Tree] = Json.format[Tree]
           *
           */

          s"implicit val jsonFormatter: Format[${classRep.classDefinitionScala}] = Json.format[${classRep.classDefinitionScala}]"
        }
      }
    }

    s"""
      case class ${classRep.classDefinitionScala}(${fieldExpressions.mkString(",")}) $extendsClass

      object ${classRep.name} {

        $formatter

      }
     """
  }


  private def generateTraitWithCompanion(topLevelClassRep: ClassRep, leafClassReps: List[ClassRep], classMap: ClassMap): String = {

    println(s"Generating trait for: ${topLevelClassRep.classDefinitionScala}")

    def leafClassRepToWithTypeHintExpression(leafClassRep: ClassRep): String = {
      s"""${leafClassRep.name}.jsonFormatter.withTypeHint("${leafClassRep.jsonTypeInfo.get.discriminatorValue.get}")"""
    }

    val extendsClass = topLevelClassRep.parentClass.map { parentClass =>
      s"extends ${classMap(parentClass).classDefinitionScala}"
    } getOrElse ""

    val fieldDefinitions = topLevelClassRep.fields map { fieldRep =>

      if (fieldRep.required) {
        s"def ${fieldRep.safeFieldNameScala}: ${fieldRep.classPointer.classDefinitionScala}"
      } else {
        s"def ${fieldRep.safeFieldNameScala}: Option[${fieldRep.classPointer.classDefinitionScala}]"
      }

    }

    topLevelClassRep.jsonTypeInfo.collect {
      case jsonTypeInfo if leafClassReps.forall(_.jsonTypeInfo.isDefined) =>
        s"""
          sealed trait ${topLevelClassRep.classDefinitionScala} $extendsClass {

            ${fieldDefinitions.mkString("\n\n")}

          }

          object ${topLevelClassRep.name} {

            implicit val jsonFormat: Format[${topLevelClassRep.classDefinitionScala}] =
              TypeHintFormat(
                "${jsonTypeInfo.discriminator}",
                ${leafClassReps.map(leafClassRepToWithTypeHintExpression).mkString(",\n")}
              )

          }
         """
    } getOrElse ""

  }


  def generateHierarchicalClassSource(hierarchyReps: List[ClassRep], classMap: ClassMap): List[ClassRep] = {

    val topLevelClass = hierarchyReps.find(_.parentClass.isEmpty).get
    // If there are no intermediary levels between the top level class and the children, then the
    // childClasses and leafClasses will be identical sets.
    val childClasses = hierarchyReps.filter(_.parentClass.isDefined)
    val leafClasses = hierarchyReps.filter(_.subClasses.isEmpty)

    val packages = hierarchyReps.groupBy(_.packageName)
    assert(
      packages.keys.size == 1,
      s"""
         |Classes in a class hierarchy must be defined in the same namespace/package. The classes
         |${hierarchyReps.map(_.name).mkString("\n")}
         |should be defined in ${topLevelClass.packageName}, but are scattered over the following packages:
         |${packages.keys.mkString("\n")}
       """.stripMargin)

    val imports: Set[String] = hierarchyReps.foldLeft(Set.empty[String]) { (importsAggr, classRp) =>
      collectImports(classRp) ++ importsAggr
    }

    val typeDiscriminator = topLevelClass.jsonTypeInfo.get.discriminator

    val source =
      s"""
        package ${topLevelClass.packageName}

        import play.api.libs.json._
        import io.atomicbits.scraml.dsl.json.TypedJson._

        ${imports.mkString("\n")}

        ${generateTraitWithCompanion(topLevelClass, leafClasses, classMap)}

        ${leafClasses.map(generateCaseClassWithCompanion(_, Some(topLevelClass), Some(typeDiscriminator))).mkString("\n\n")}
     """

    // The implementations sits completely in the top level class, so the source of the child classes remains empty.
    topLevelClass.withContent(source) +: childClasses
  }

}
