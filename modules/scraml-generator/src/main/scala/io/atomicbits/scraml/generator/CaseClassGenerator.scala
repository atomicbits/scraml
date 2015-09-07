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
object CaseClassGenerator {


  def generateCaseClasses(schemaLookup: SchemaLookup): List[ClassRep] = {

    // Expand all canonical names into their case class definitions.

    val (classRepsInHierarcy, classRepsStandalone) = schemaLookup.classReps.values.toList.partition(_.isInHierarchy)

    val classHierarchies = classRepsInHierarcy.groupBy(_.topLevelParent).collect { case (Some(classRep), reps) => (classRep, reps) }

    classHierarchies.values.toList.map(generateHierarchicalClassReps(_, schemaLookup)) :::
      classRepsStandalone.map(generateNonHierarchicalClassRep(_, schemaLookup)) collect { case clRep if clRep.content.isDefined => clRep }
  }


  /**
   * Collect all type imports for a given class and its generic types, but not its parent or child classes.
   */
  def collectImports(collectClassRep: ClassRep): Set[String] = {

    val ownPackage = collectClassRep.packageName

    /**
     * Collect the type imports for the given class rep without recursing into the field types.
     */
    def collectTypeImports(collected: Set[String], classRp: ClassRep): Set[String] = {

      val collectedWithClassRep =
        if (classRp.packageName != ownPackage && !classRp.predef) collected + s"import ${classRp.fullyQualifiedName}"
        else collected

      classRp.types.foldLeft(collectedWithClassRep)(collectTypeImports)

    }

    val ownTypeImports: Set[String] = collectTypeImports(Set.empty, collectClassRep)

    collectClassRep.fields.map(_.classRep).foldLeft(ownTypeImports)(collectTypeImports)

  }


  def generateNonHierarchicalClassRep(classRep: ClassRep,
                                      schemaLookup: SchemaLookup): ClassRep = {

    println(s"Generating case class for: ${classRep.classDefinition}")

    val imports: Set[String] = collectImports(classRep)

    val fieldExpressions = classRep.fields.sortBy(!_.required).map(_.fieldExpression)

    val source =
      s"""
        package ${classRep.packageName}

        import play.api.libs.json.{Format, Json}

        ${imports.mkString("\n")}

        ${generateCaseClassWithCompanion(classRep)}
     """

    classRep.withContent(content = source)
  }


  private def generateCaseClassWithCompanion(classRep: ClassRep): String = {

    val fieldExpressions = classRep.fields.sortBy(!_.required).map(_.fieldExpression)

    s"""
      case class ${classRep.classDefinition}(${fieldExpressions.mkString(",")})

      object ${classRep.name} {

        implicit val jsonFormatter: Format[${classRep.classDefinition}] = Json.format[${classRep.classDefinition}]

      }
     """
  }


  private def generateTraitWithCompanion(topLevelClassRep: ClassRep, leafClassReps: List[ClassRep]): String = {

    def leafClassRepToWithTypeHintExpression(leafClassRep: ClassRep): String = {
      s"""${leafClassRep.name}.jsonFormatter.withTypeHint("${leafClassRep.jsonTypeInfo.get.discriminatorValue}")"""
    }

    val extendsClass = topLevelClassRep.parentClass.map(parentClass => s"extends ${parentClass.classDefinition}").getOrElse("")

    topLevelClassRep.jsonTypeInfo.collect {
      case jsonTypeInfo if leafClassReps.forall(_.jsonTypeInfo.isDefined) =>
        s"""
          sealed trait ${topLevelClassRep.classDefinition} $extendsClass {

          }

          object ${topLevelClassRep.name} {

            implicit val jsonFormat: Format[${topLevelClassRep.classDefinition}] =
              TypeHintFormat(
                ${jsonTypeInfo.discriminator},
                ${leafClassReps.map(leafClassRepToWithTypeHintExpression).mkString(",\n")}
              )

          }
         """
    } getOrElse ""


  }


  def generateHierarchicalClassReps(hierarchyReps: List[ClassRep], schemaLookup: SchemaLookup): ClassRep = {

    val topLevelClass = hierarchyReps.head.topLevelParent.get

    val packages = hierarchyReps.groupBy(_.packageName)
    assert(
      packages.keys.size == 1,
      s"""
         |Classes in a class hierarchy must be defined in the same namespace/package. The classes
         |${hierarchyReps.map(_.name).mkString("\n")}
          |should be defined in ${topLevelClass.packageName}, but are scattered over the following packages:
                                                              |${packages.keys.mkString("\n")}
       """.stripMargin)

    val leafClasses = hierarchyReps.filter(_.subClasses.isEmpty)

    val imports: Set[String] = hierarchyReps.foldLeft(Set.empty[String]) { (importsAggr, classRp) =>
      collectImports(classRp) ++ importsAggr
    }

    val source =
      s"""
        package ${topLevelClass.packageName}

        import play.api.libs.json.{Format, Json}
        import io.atomicbits.scraml.dsl.json.TypedJson._

        ${imports.mkString("\n")}

        ${generateTraitWithCompanion(topLevelClass, leafClasses)}

        ${leafClasses.map(generateCaseClassWithCompanion).mkString("\n\n")}
     """

    topLevelClass.withContent(source)
  }

}
