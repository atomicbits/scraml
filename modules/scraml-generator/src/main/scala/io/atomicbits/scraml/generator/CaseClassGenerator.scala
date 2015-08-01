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

import io.atomicbits.scraml.generator.lookup.{ObjectElExt, SchemaLookup}

import scala.language.experimental.macros
import scala.reflect.macros.whitebox


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

  def generateCaseClasses(schemaLookup: SchemaLookup): List[String] = {

    // Expand all canonical names into their case class definitions.

    val caseClasses = schemaLookup.objectMap.keys.toList.map { key =>
      generateCaseClassWithCompanionObject(
        schemaLookup.canonicalNames(key).name,
        schemaLookup.objectMap(key),
        schemaLookup,
        c
      )
    }

    caseClasses.flatten

  }

  def generateCaseClassWithCompanionObject(canonicalName: String,
                                           objectEl: ObjectElExt, schemaLookup: SchemaLookup, c: whitebox.Context): List[c.universe.Tree] = {

    println(s"Generating case class for: $canonicalName")

    import c.universe._

    val className = TypeName(canonicalName)
    val classAsTermName = TermName(canonicalName)
    val caseClassFields = objectEl.properties.toList.map(TypeGenerator.schemaAsField(_, objectEl.requiredFields, schemaLookup, c))

    List(
      q"""
       case class $className(..$caseClassFields)
     """,
      q"""
       object $classAsTermName {

         implicit val jsonFormatter: Format[$className] = Json.format[$className]

       }
     """)

  }

}
