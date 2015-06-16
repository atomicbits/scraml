/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.jsonschemaparser.{AbsoluteId, SchemaLookup}
import io.atomicbits.scraml.jsonschemaparser.model._

import scala.reflect.macros.whitebox
import scala.language.experimental.macros


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

  def generateCaseClasses(schemaLookup: SchemaLookup, c: whitebox.Context) = {

    // Expand all canonical names into their case class definitions.

    val caseClasses = schemaLookup.objectMap.keys.toList.map { key =>
      generateCaseClassWithCompanionObject(
        schemaLookup.canonicalNames(key),
        schemaLookup.objectMap(key),
        schemaLookup,
        c
      )
    }

    caseClasses.flatten

  }

  def generateCaseClassWithCompanionObject(canonicalName: String,
                                           objectEl: ObjectEl,
                                           schemaLookup: SchemaLookup,
                                           c: whitebox.Context): List[c.universe.Tree] = {

    def toField(property: (String, Schema), requiredFields: List[String]): c.universe.Tree = {

      val (propertyName, schema) = property
      val cleanName = cleanFieldName(propertyName)

      val absoluteId = schema.id match {
        case absId: AbsoluteId => absId
        case _ => sys.error("All schema references must be absolute for case class generation.")
      }

      val field =
        schema match {
          case objField: AllowedAsObjectField =>
            val required = requiredFields.contains(propertyName) || objField.required
            expandFieldName(cleanName, fetchTypeNameFor(objField, schemaLookup), required, c)
          case noObjectField =>
            sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
        }

      field
    }

    import c.universe._

    val className = TypeName(canonicalName)
    val classAsTermName = TermName(canonicalName)
    val caseClassFields: List[c.universe.Tree] = objectEl.properties.toList.map(toField(_, objectEl.requiredFields))

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


  private def expandFieldName(fieldName: String,
                              typeName: String,
                              required: Boolean,
                              c: whitebox.Context): c.universe.Tree = {
    import c.universe._

    val nameTermName = TermName(fieldName)
    val typeTypeName = TypeName(typeName)

    if (required) {
      q"val $nameTermName: $typeTypeName"
    } else {
      q"val $nameTermName: Option[$typeTypeName]"
    }
  }


  private def expandTypedArray(arrayEl: ArrayEl, schemaLookup: SchemaLookup): String = {

    s"List[${fetchTypeNameFor(arrayEl.items, schemaLookup)}]"

  }


  private def fetchTypeNameFor(schema: Schema, schemaLookup: SchemaLookup): String = {

    // ToDo: this code to get the absolute id appears everywhere, we must find a way to refactor this!
    val absoluteId = schema.id match {
      case absId: AbsoluteId => absId
      case _ => sys.error("All schema references must be absolute for case class generation.")
    }

    schema match {
      case objEl: ObjectEl => schemaLookup.canonicalNames(absoluteId)
      case arrEl: ArrayEl => s"List[${fetchTypeNameFor(arrEl, schemaLookup)}]"
      case stringEl: StringEl => "String"
      case numberEl: NumberEl => "Double"
      case integerEl: IntegerEl => "Int"
      case booleanEl: BooleanEl => "Boolean"
      case schemaRef: SchemaReference => fetchTypeNameFor(schemaLookup.lookupSchema(schemaRef.refersTo), schemaLookup)
      case enumEl: EnumEl => schemaLookup.canonicalNames(absoluteId)
      case otherSchema => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a List type parameter.")
    }
  }


  private def cleanFieldName(propertyName: String): String = {

    def unCapitalize(name: String): String =
      if (name.length == 0) ""
      else if (name.charAt(0).isLower) name
      else {
        val chars = name.toCharArray
        chars(0) = chars(0).toLower
        new String(chars)
      }

    // capitalize after special characters and drop those characters along the way
    val capitalizedAfterDropChars =
      List('-', '_', '+', ' ').foldLeft(propertyName) { (cleaned, dropChar) =>
        cleaned.split(dropChar).filter(_.nonEmpty).map(_.capitalize).mkString("")
      }
    // capitalize after numbers 0 to 9, but keep the numbers
    val capitalized =
      (0 to 9).map(_.toString.head).toList.foldLeft(capitalizedAfterDropChars) { (cleaned, numberChar) =>
        // Make sure we don't drop the occurrences of numberChar at the end by adding a space and removing it later.
        val cleanedWorker = s"$cleaned "
        cleanedWorker.split(numberChar).map(_.capitalize).mkString(numberChar.toString).stripSuffix(" ")
      }
    // Make the first charachter a lower case to get a camel case name and do some
    // final cleanup of all strange characters
    unCapitalize(capitalized).replaceAll("[^A-Za-z0-9]", "")
  }

}
