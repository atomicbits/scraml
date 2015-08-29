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

    schemaLookup.objectMap.keys.toList.map { key =>
      generateCaseClassWithCompanionObject(schemaLookup.classReps(key), schemaLookup)
    }

  }


  def generateCaseClassWithCompanionObject(classRep: ClassRep,
                                           schemaLookup: SchemaLookup): ClassRep = {

    println(s"Generating case class for: ${classRep.classDefinition}")

    def collectImports(): Set[String] = {

      val ownPackage = classRep.packageName

      /**
       * Collect the type imports for the given class rep without recursing into the field types.
       */
      def collectTypeImports(collected: Set[String], classRp: ClassRep): Set[String] = {

        val collectedWithClassRep =
          if (classRp.packageName != ownPackage) collected + s"import ${classRp.fullyQualifiedName}"
          else collected

        classRp.types.foldLeft(collectedWithClassRep)(collectTypeImports)

      }

      val ownTypeImports: Set[String] = collectTypeImports(Set.empty, classRep)

      classRep.fields.map(_.classRep).foldLeft(ownTypeImports)(collectTypeImports)

    }

    val imports: Set[String] = collectImports()

    val fieldExpressions = classRep.fields.map(_.fieldExpression)

    val caseClassSource =
      s"""
        package ${classRep.packageName}

        import play.api.libs.json.{Format, Json}

        ${imports.mkString("\n")}

        case class ${classRep.classDefinition}(${fieldExpressions.mkString(",")})

        object ${classRep.name} {

          implicit val jsonFormatter: Format[${classRep.classDefinition}] = Json.format[${classRep.classDefinition}]

        }
    """

    classRep.withContent(content = caseClassSource)

  }

}
