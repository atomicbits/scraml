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

import io.atomicbits.scraml.generator.codegen.CaseClassGenerator._
import io.atomicbits.scraml.generator.model.{ClassReferenceAsFieldRep, EnumValuesClassRep, ClassRep}
import io.atomicbits.scraml.generator.model.ClassRep._

/**
 * Created by peter on 30/09/15.
 */
object PojoGenerator {

  def generatePojos(classMap: ClassMap): List[ClassRep] = {

    val (classRepsInHierarcy, classRepsStandalone) = classMap.values.toList.partition(_.isInHierarchy)

    val classHierarchies = classRepsInHierarcy.groupBy(_.hierarchyParent(classMap))
      .collect { case (Some(classRep), reps) => (classRep, reps) }

    classHierarchies.values.toList.flatMap(generateHierarchicalClassReps(_, classMap)) :::
      classRepsStandalone.map(generateNonHierarchicalClassRep(_, classMap))
  }


  def generateHierarchicalClassReps(hierarchyReps: List[ClassRep], classMap: ClassMap): List[ClassRep] = {

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

    val typeDiscriminator = topLevelClass.jsonTypeInfo.get.discriminator

    val topLevelImports: Set[String] = collectImports(topLevelClass)

    val classesWithDiscriminators =
      childClasses.flatMap(childClass => childClass.jsonTypeInfo.flatMap(_.discriminatorValue).map((childClass, _)))

    val jsonSubTypes =
      classesWithDiscriminators map {
        case (classRep, discriminator) =>
          s"""
             @JsonSubTypes.Type(value = ${classRep.name}.class, name = "$discriminator")
           """
      }

    val jsonTypeInfo =
      s"""
         @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$typeDiscriminator")
         @JsonSubTypes({
                 ${jsonSubTypes.mkString(",\n")}
         })
       """

    val topLevelSource =
      s"""
        package ${topLevelClass.packageName};

        import org.codehaus.jackson.annotate.JsonSubTypes;
        import org.codehaus.jackson.annotate.JsonTypeInfo;

        ${topLevelImports.mkString(";\n")};

        $jsonTypeInfo
        ${generatePojoSource(topLevelClass)}

     """

    topLevelClass.withContent(topLevelSource) +: childClasses.map(generateNonHierarchicalClassRep(_, classMap))
  }


  def generateNonHierarchicalClassRep(classRep: ClassRep, classMap: ClassMap): ClassRep = {

    println(s"Generating case class for: ${classRep.classDefinitionScala}")


    classRep match {
      case e: EnumValuesClassRep => generateEnumClassRep(e)
      case _                     => generateNonEnumClassRep(classRep)
    }
  }


  private def generateEnumClassRep(classRep: EnumValuesClassRep): ClassRep = {

    val source =
      s"""
        public enum ${classRep.name} {

          ${classRep.values.mkString(",\n")}

        }
     """

    classRep.withContent(content = source)
  }


  private def generateNonEnumClassRep(classRep: ClassRep): ClassRep = {

    val imports: Set[String] = collectImports(classRep)

    val fieldExpressions = classRep.fields.sortBy(!_.required).map(_.fieldExpressionScala)

    val source =
      s"""
        package ${classRep.packageName};

        ${imports.mkString(";\n")};

        ${generatePojoSource(classRep)}
     """

    classRep.withContent(content = source)
  }


  private def generatePojoSource(classRep: ClassRep,
                                 parentClassRep: Option[ClassRep] = None,
                                 skipFieldName: Option[String] = None): String = {

    val selectedFields =
      skipFieldName map { skipField =>
        classRep.fields.filterNot(_.fieldName == skipField)
      } getOrElse classRep.fields

    val sortedFields = selectedFields.sortBy(!_.required)
    val fieldExpressions = sortedFields.map(_.fieldExpressionJava)

    val privateFieldExpressions = fieldExpressions.map(fe => s"private $fe;")

    val getterAndSetters = sortedFields map {
      case ClassReferenceAsFieldRep(fieldName, classPointer, required) =>
        val fieldNameCap = fieldName.capitalize
        s"""
           public ${classPointer.classDefinitionJava} get$fieldNameCap() {
             return $fieldName;
           }

           public void set$fieldNameCap(${classPointer.classDefinitionJava} $fieldName) {
             this.$fieldName = $fieldName;
           }

         """
    }

    val extendsClass = parentClassRep.map(parentClassRep => s"extends ${parentClassRep.classDefinitionJava}").getOrElse("")

    val constructorInitialization = sortedFields map { sf =>
      val fieldName = sf.fieldName
      s"""this.$fieldName = $fieldName;"""
    }


    s"""
      public class ${classRep.classDefinitionJava} $extendsClass

      ${privateFieldExpressions.mkString("\n")}

      public ${classRep.name}() {
      }

      public ${classRep.name}(${fieldExpressions.mkString(", ")}) {
        ${constructorInitialization.mkString("\n")}
      }

      ${getterAndSetters.mkString("\n")}

     """
  }

}
