package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 3/06/15, Atomic BITS (http://atomicbits.io). 
 */
object CanonicalNameGenerator {

  def deduceCanonicalNames(schemaLookup: SchemaLookup): SchemaLookup = {

    val schemaPaths = schemaLookup.lookupTable.keys.map(SchemaPath(_)).toList

    val groupedByHasFragment = schemaPaths.groupBy(_.reverseFragment.isEmpty)

    val schemaPathsWithoutFragment = groupedByHasFragment.getOrElse(true, Nil).sortBy(_.reversePath.length)
    val schemaPathsWithFragment = groupedByHasFragment.getOrElse(false, Nil).sortBy(_.reverseFragment.length)

    // First find canonical names for schema paths without their fragments.

    // Map from schema origins to their canonical names
    type CanonicalMap = Map[String, String]

    val canonicalMap: CanonicalMap = Map.empty

    val canonicalBasePaths =
      schemaPathsWithoutFragment.foldLeft(canonicalMap) { (canMap, schemaPath) =>

        val taken = canMap.values.toList

        def deduce(reversePath: List[String], suffix: String = ""): CanonicalMap = {
          reversePath match {
            case p :: ps =>
              val canonicalProposal = s"${p.capitalize}$suffix"
              if (!taken.contains(canonicalProposal)) canMap + (schemaPath.origin -> canonicalProposal)
              else deduce(ps, canonicalProposal)
            case Nil =>
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
        val basePath = schemaPath.origin.split("#").head
        val canonicalBase =
          canMap.getOrElse(
            basePath,
            throw new IllegalArgumentException(s"Base path $basePath not found for ${schemaPath.origin}.")
          )

        def deduce(reverseFragment: List[String], suffix: String = ""): CanonicalMap = {
          reverseFragment match {
            case f :: fs =>
              val canonicalProposal = s"${f.capitalize}$suffix"
              if (!taken.contains(canonicalProposal)) canMap + (schemaPath.origin -> canonicalProposal)
              else if (!taken.contains(s"$canonicalBase$canonicalProposal"))
                canMap + (schemaPath.origin -> s"$canonicalBase$canonicalProposal")
              else deduce(fs, canonicalProposal)
            case Nil =>
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
case class SchemaPath(reversePath: List[String], reverseFragment: List[String], origin: String)

object SchemaPath {

  def apply(origin: String): SchemaPath = {

    val withoutProtocol = origin.split("://").drop(1).head
    val (pathPart, fragmentPart) =
      withoutProtocol.split("#").toList match {
        case path :: Nil => (path, None)
        case path :: fragment => (path, Some(fragment.head))
      }

    val reverseRelativePath = pathPart.split("/").drop(1).toList.reverse
    val reverseFragmentPath = fragmentPart.map(_.split("/").toList.reverse).getOrElse(Nil).filter(_.nonEmpty)

    // E.g. when the origin is: http://my.site/schemas/myschema.json#/definitions/schema2
    // then:
    // reverseRelativePath = List("myschema.json", "schemas")
    // reverseFragmentPath = List("schema2", "definitions")

    // cleanup of the head of reverseRelativePath
    val cleanReverseRelativePath = reverseRelativePath match {
      case fileName :: path => cleanFileName(fileName) :: path
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
