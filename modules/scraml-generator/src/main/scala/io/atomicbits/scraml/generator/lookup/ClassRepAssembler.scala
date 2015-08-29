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

package io.atomicbits.scraml.generator.lookup

import io.atomicbits.scraml.generator.{CleanNameUtil, ClassAsFieldRep, ClassRep}
import io.atomicbits.scraml.jsonschemaparser._
import io.atomicbits.scraml.jsonschemaparser.model.{AllowedAsObjectField, Schema}

/**
*  Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io).
*/
object ClassRepAssembler {

  type CanonicalMap = Map[AbsoluteId, ClassRep]


  def deduceClassReps(schemaLookup: SchemaLookup): SchemaLookup = {

    val canonicalMap: CanonicalMap = deduceCanonicalNames(schemaLookup.objectMap.keys.toList)

    val canonicalMapWithFields: CanonicalMap = addFields(canonicalMap, schemaLookup)

    schemaLookup.copy(classReps = canonicalMapWithFields)
  }

  /**
   *
   * @param ids: The absolute IDs for which to generate class representations.
   * @return A map containing the class representation for each absolute ID.
   */
  def deduceCanonicalNames(ids: List[AbsoluteId]): CanonicalMap = {

    val schemaPaths: List[SchemaClassReference] = ids.map(SchemaClassReference(_))

    // Group all schema references by their paths. These paths are going to define the package structure,
    // so each class name will need to be unique within its package.
    val packageGroups: List[List[SchemaClassReference]] = schemaPaths.groupBy(_.path).values.toList

    def schemaReferenceToCanonicalName(canonicalMap: CanonicalMap, schemaReference: SchemaClassReference): CanonicalMap = {

      val className = schemaReference.fragment.foldLeft(schemaReference.className) { (classNm, fragmentPart) =>
        s"$classNm${CleanNameUtil.cleanClassName(fragmentPart)}"
      }

      val classRep = ClassRep(name = className, packageParts = schemaReference.path)

      canonicalMap + (schemaReference.origin -> classRep)
    }

    def packageGroupToCanonicalNames(canonicalMap: CanonicalMap, packageGroup: List[SchemaClassReference]): CanonicalMap =
      packageGroup.foldLeft(canonicalMap)(schemaReferenceToCanonicalName)

    val canonicalMap: CanonicalMap = Map.empty

    packageGroups.foldLeft(canonicalMap)(packageGroupToCanonicalNames)
  }

  def addFields(canonicalMap: CanonicalMap, schemaLookup: SchemaLookup): CanonicalMap = {

    def schemaAsField(property: (String, Schema), requiredFields: List[String]): ClassAsFieldRep = {

      val (propertyName, schema) = property

      schema match {
        case objField: AllowedAsObjectField =>
          val required = requiredFields.contains(propertyName) || objField.required
          ClassAsFieldRep(propertyName, schemaLookup.schemaAsClassRep(objField), required)
        case noObjectField                  =>
          sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
      }

    }

    canonicalMap map { idAndClassRep =>
      val (id, classRep) = idAndClassRep

      val objectEl = schemaLookup.objectMap(id)

      val fields: List[ClassAsFieldRep] = objectEl.properties.toList.map(schemaAsField(_, objectEl.requiredFields))

      val classRepWithFields = classRep.withFields(fields)

      (id, classRepWithFields)
    }

  }

}

/**
 * Helper case class for the canonical name generator.
 *
 * @param className The file name in the schema path that is cleaned up to be used as a class name.
 * @param path The relative path of the schema ID, without the file name itself.
 * @param fragment The fragment path of the schema ID.
 * @param origin The original schema ID.
 */
case class SchemaClassReference(className: String, path: List[String], fragment: List[String], origin: AbsoluteId)

object SchemaClassReference {

  def apply(origin: AbsoluteId): SchemaClassReference = {

    val hostPathReversed = origin.hostPath.reverse
    val relativePath = origin.rootPath.dropRight(1)
    val originalFileName = origin.rootPath.takeRight(1).head
    val fragmentPath = origin.fragments

    // E.g. when the origin is: http://atomicbits.io/api/schemas/myschema.json#/definitions/schema2
    // then:
    // hostPathReversed = List("io", "atomicbits")
    // relativePath = List("api", "schemas")
    // originalFileName = "myschema.json"
    // fragmentPath = List("definitions", "schema2")

    SchemaClassReference(
      className = CleanNameUtil.cleanClassNameFromFileName(originalFileName),
      path = hostPathReversed ++ relativePath,
      fragment = fragmentPath,
      origin = origin
    )
  }

}
