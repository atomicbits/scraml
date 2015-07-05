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

import io.atomicbits.scraml.jsonschemaparser._

/**
 * Created by peter on 5/07/15. 
 */
/**
 * Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io).
 */
object CanonicalNameGenerator {

  /**
   * Deduce the canonical names from the schema ids. Currently, only PlainClassReps are created. Later, when supporting typed
   * classes, we'll have to add a second run that upgrades the PlainClassReps to TypeClassReps where necessary.
   *
   * @param schemaLookup The schema lookup.
   * @return The updated schema lookup.
   */
  def deduceCanonicalNames(schemaLookup: SchemaLookup): SchemaLookup = {

    val schemaPaths: List[SchemaPath] =
      schemaLookup.objectMap.toList.collect {
        case (absId, _) => SchemaPath(absId)
      } ++ schemaLookup.enumMap.toList.collect {
        case (absId, _) => SchemaPath(absId)
      }

    val (groupedByEmptyFragment, groupedByNonEmptyFragment) = schemaPaths.partition(_.reverseFragment.isEmpty)

    val schemaPathsWithoutFragment = groupedByEmptyFragment.sortBy(_.reversePath.length)
    val schemaPathsWithFragment = groupedByNonEmptyFragment.sortBy(_.reverseFragment.length)

    // First find canonical names for schema paths without their fragments.

    // Map from schema origins to their canonical names
    type CanonicalMap = Map[AbsoluteId, ClassRep]

    val canonicalMap: CanonicalMap = Map.empty

    val canonicalBasePaths =
      schemaPathsWithoutFragment.foldLeft(canonicalMap) { (canMap, schemaPath) =>

        val taken = canMap.values.toList

        def deduce(reversePath: List[String], suffix: String = ""): CanonicalMap = {
          reversePath match {
            case p :: ps =>
              val canonicalProposal = s"${p.capitalize}$suffix"
              if (!taken.contains(canonicalProposal)) canMap + (schemaPath.origin -> PlainClassRep(canonicalProposal))
              else deduce(ps, canonicalProposal)
            case Nil     =>
              throw new IllegalArgumentException(s"Cannot deduce a canonical name for schema ${schemaPath.origin}")
          }
        }

        deduce(schemaPath.reversePath)
      }

    // Then deduce the canonical names for the schema paths with fragments, using their base path canonical
    // name when necessary.

    val canonicals =
      schemaPathsWithFragment.foldLeft(canonicalBasePaths) { (canMap, schemaPath) =>

        val taken = canMap.values.toList
        val basePath = schemaPath.origin.rootPart
        val canonicalBase =
          canMap.getOrElse(
            basePath,
            throw new IllegalArgumentException(s"Base path $basePath not found for ${schemaPath.origin}.")
          )

        def deduce(reverseFragment: List[String], suffix: String = ""): CanonicalMap = {
          reverseFragment match {
            case f :: fs =>
              val canonicalProposal = s"${f.capitalize}$suffix"
              if (!taken.contains(canonicalProposal)) canMap + (schemaPath.origin -> PlainClassRep(canonicalProposal))
              else if (!taken.contains(s"$canonicalBase$canonicalProposal"))
                canMap + (schemaPath.origin -> PlainClassRep(s"$canonicalBase$canonicalProposal"))
              else deduce(fs, canonicalProposal)
            case Nil     =>
              throw new IllegalArgumentException(s"Cannot deduce a canonical name for schema ${schemaPath.origin}")
          }
        }

        deduce(schemaPath.reverseFragment)

      }

    schemaLookup.copy(canonicalNames = canonicals)
  }

}

/**
 * Helper case class for the canonical name generator.
 *
 * @param reversePath The relative path of the schema ID reversed.
 * @param reverseFragment The fragment path of the schema ID reversed.
 * @param origin The original schema ID.
 */
case class SchemaPath(reversePath: List[String], reverseFragment: List[String], origin: AbsoluteId)

object SchemaPath {

  def apply(origin: AbsoluteId): SchemaPath = {

    val reverseRelativePath = origin.rootPath.reverse
    val reverseFragmentPath = origin.fragments.reverse

    // E.g. when the origin is: http://my.site/schemas/myschema.json#/definitions/schema2
    // then:
    // reverseRelativePath = List("myschema.json", "schemas")
    // reverseFragmentPath = List("schema2", "definitions")

    // cleanup of the head of reverseRelativePath
    val cleanReverseRelativePath = reverseRelativePath match {
      case fileName :: path => cleanFileName(fileName) :: path
      case Nil              => sys.error("A relative path must have a path and file name.")
    }

    SchemaPath(reversePath = cleanReverseRelativePath, reverseFragment = reverseFragmentPath, origin = origin)
  }

  private def cleanFileName(fileName: String): String = {
    val withOutExtension = fileName.split('.').filter(_.nonEmpty).head
    // capitalize after special characters and drop those characters along the way
    val capitalizedAfterDropChars =
      List('-', '_', '+', ' ').foldLeft(withOutExtension) { (cleaned, dropChar) =>
        cleaned.split(dropChar).filter(_.nonEmpty).map(_.capitalize).mkString("")
      }
    // capitalize after numbers 0 to 9, but keep the numbers
    val capitalized =
      (0 to 9).map(_.toString.head).toList.foldLeft(capitalizedAfterDropChars) { (cleaned, numberChar) =>
        // Make sure we don't drop the occurrences of numberChar at the end by adding a space and removing it later.
        val cleanedWorker = s"$cleaned "
        cleanedWorker.split(numberChar).map(_.capitalize).mkString(numberChar.toString).stripSuffix(" ")
      }
    // final cleanup of all strange characters
    capitalized.replaceAll("[^A-Za-z0-9]", "")
  }

}
