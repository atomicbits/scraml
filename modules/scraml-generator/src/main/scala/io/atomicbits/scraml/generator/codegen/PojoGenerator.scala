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
import io.atomicbits.scraml.generator.model.{Field, EnumValuesClassRep, ClassRep}
import io.atomicbits.scraml.generator.model.ClassRep._
import io.atomicbits.scraml.generator.util.CleanNameUtil

/**
 * Created by peter on 30/09/15.
 */
object PojoGenerator {

  def generatePojos(classMap: ClassMap): List[ClassRep] = {

    val (classRepsInHierarcy, classRepsStandalone) = classMap.values.toList.partition(_.isInHierarchy)

    val classHierarchies = classRepsInHierarcy.groupBy(_.hierarchyParent(classMap))
      .collect { case (Some(classRep), reps) => (classRep, reps) }

    classHierarchies.values.toList.flatMap(generateHierarchicalClassReps(_, classMap)) :::
      classRepsStandalone.map(generateNonHierarchicalClassRep(_, None, classMap))
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

        import com.fasterxml.jackson.annotation.*;

        ${topLevelImports.mkString("", ";\n", ";")};

        $jsonTypeInfo
        ${generatePojoSource(topLevelClass, None, Some(typeDiscriminator), isAbstract = true)}

     """

    topLevelClass.withContent(topLevelSource) +:
      childClasses.map(generateNonHierarchicalClassRep(_, Some(topLevelClass), classMap, Some(typeDiscriminator)))
  }


  def generateNonHierarchicalClassRep(classRep: ClassRep,
                                      parentClassRep: Option[ClassRep] = None,
                                      classMap: ClassMap,
                                      skipField: Option[String] = None): ClassRep = {

    classRep match {
      case e: EnumValuesClassRep                          => generateEnumClassRep(e)
      case x if !x.classRef.library && !x.classRef.predef => generateNonEnumClassRep(x, parentClassRep, skipField)
      case y                                              => y
    }
  }


  private def generateEnumClassRep(classRep: EnumValuesClassRep): ClassRep = {

    // Accompany the enum names with their 'Java-safe' name.
    val enumsWithSafeName =
      classRep.values map { value =>
        val safeName = CleanNameUtil.escapeJavaKeyword(CleanNameUtil.cleanEnumName(value))
        s"""$safeName("$value")""" // e.g. space("spa ce")
      }

    val classNameCamel = CleanNameUtil.camelCased(classRep.name)

    val source =
      s"""
        package ${classRep.packageName};

        import com.fasterxml.jackson.annotation.*;

        public enum ${classRep.name} {

          ${enumsWithSafeName.mkString("", ",\n", ";\n")}

          private final String value;

          private ${classRep.name}(final String value) {
                  this.value = value;
          }

          @JsonValue
          final String value() {
            return this.value;
          }

          @JsonCreator
          public static ${classRep.name} fromValue(String value) {
            for (${classRep.name} $classNameCamel : ${classRep.name}.values()) {
              if (value.equals($classNameCamel.value())) {
                return $classNameCamel;
              }
            }
            throw new IllegalArgumentException("Cannot instantiate a ${classRep.name} enum element from " + value);
          }

        }
       """

    println(s"Generating enum for: ${classRep.classDefinitionJava}")
    classRep.withContent(content = source)
  }


  private def generateNonEnumClassRep(classRep: ClassRep, parentClassRep: Option[ClassRep] = None, skipField: Option[String] = None): ClassRep = {

    val imports: Set[String] = collectImports(classRep)

    val fieldExpressions = classRep.fields.sortBy(!_.required).map(_.fieldExpressionScala)

    val source =
      s"""
        package ${classRep.packageName};

        ${imports.mkString("", ";\n", ";")}

        import java.util.*;
        import com.fasterxml.jackson.annotation.*;

        ${generatePojoSource(classRep, parentClassRep, skipField)}
     """

    println(s"Generating POJO for: ${classRep.classDefinitionJava}")
    classRep.withContent(content = source)
  }


  private def generatePojoSource(classRep: ClassRep,
                                 parentClassRep: Option[ClassRep] = None,
                                 skipFieldName: Option[String] = None, isAbstract: Boolean = false): String = {

    val selectedFields =
      skipFieldName map { skipField =>
        classRep.fields.filterNot(_.fieldName == skipField)
      } getOrElse classRep.fields

    val sortedFields = selectedFields.sortBy(_.safeFieldNameJava) // In Java Pojo's, we sort by field name!

    val privateFieldExpressions = sortedFields.map { field =>
      s"""
           @JsonProperty(value = "${field.fieldName}")
           private ${field.fieldExpressionJava};
         """
    }


    val getterAndSetters = sortedFields map {
      case fieldRep@Field(fieldName, classPointer, required) =>
        val fieldNameCap = fieldRep.safeFieldNameJava.capitalize
        s"""
           public ${classPointer.classDefinitionJava} get$fieldNameCap() {
             return ${fieldRep.safeFieldNameJava};
           }

           public void set$fieldNameCap(${classPointer.classDefinitionJava} ${fieldRep.safeFieldNameJava}) {
             this.${fieldRep.safeFieldNameJava} = ${fieldRep.safeFieldNameJava};
           }

         """
    }

    val extendsClass = classRep.parentClass.map(parentClassRep => s"extends ${parentClassRep.classDefinitionJava}").getOrElse("")

    val fieldsWithParentFields =
      parentClassRep map { parentClass =>
        (sortedFields ++ parentClass.fields).sortBy(_.safeFieldNameJava)
      } getOrElse sortedFields

    val constructorInitialization = fieldsWithParentFields map { sf =>
      val fieldNameCap = sf.safeFieldNameJava.capitalize
      s"this.set$fieldNameCap(${sf.safeFieldNameJava});"
    }

    val fieldExpressions = fieldsWithParentFields.map(_.fieldExpressionJava)

    val fieldConstructor =
      if (fieldExpressions.nonEmpty)
        s"""
          public ${classRep.name}(${fieldExpressions.mkString(", ")}) {
            ${constructorInitialization.mkString("\n")}
          }
         """
      else ""

    val classTypeDef = if (isAbstract) "abstract class" else "class"

    s"""
      public $classTypeDef ${classRep.classDefinitionJava} $extendsClass {

        ${privateFieldExpressions.mkString("\n")}

        public ${classRep.name}() {
        }

        $fieldConstructor

        ${getterAndSetters.mkString("\n")}

      }
     """
  }

}
