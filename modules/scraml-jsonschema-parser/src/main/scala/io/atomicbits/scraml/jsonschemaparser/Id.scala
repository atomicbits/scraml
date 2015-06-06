package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */

/**
 * Base class for a schema id.
 * In a correctly shaped schema, all schema ids can be expanded to their root-form.
 */
sealed trait Id

object Id {

  def apply(id: String): Id = {

  }

}

/**
 * An absolute id uniquely identifies a schema. A schema with an absolute id is the root for its child-schemas that
 * don't have an absolute or relative id.
 * An absolute id is of the form "http://atomicbits.io/schema/User.json" and often it ends with a "#".
 *
 * @param id The string representation of the id
 */
case class AbsoluteId(id: String) extends Id {

  lazy val anchor: String = id.split('/').toList.dropRight(1).mkString("/")

  def toAbsolute(relative: RelativeId) = AbsoluteId(s"$anchor/${relative.id}")

  def toAbsolute(fragment: FragmentId) = AbsoluteId(s"$id${fragment.id}")

  def expandRef(ref: String): String = {

    val refTrimmed = ref.trim

    if (refTrimmed.contains("://")) refTrimmed.stripSuffix("#") // Absolute
    else if (refTrimmed.startsWith("#")) s"$id$refTrimmed" // Fragment
    else s"$anchor/$refTrimmed" // Relative

  }

}

/**
 * A relative id identifies its schema uniquely when expanded with the anchor of its root schema. Its root schema
 * is its nearest parent that has an absolute id. A schema with a relative id is the root for its child-schemas that
 * don't have an absolute or relative id.
 * A relative id is of the form "contact/ShippingAddress.json".
 *
 * @param id The string representation of the id
 */
case class RelativeId(id: String) extends Id

/**
 * A fragment id identifies its schema uniquely by the schema path (JSON path in the original JSON representation)
 * from its nearest root schema towards itself. In other words, the fragment id should always match this schema
 * path and is redundant from that point of view.
 * It is of the form "#/some/schema/path/license"
 *
 * Mind that this parser never expands a schema fragment id to its absolute form. The absolute form of a fragment id
 * is only used in references (e.g.: { "$ref": "http://atomicbits.io/schema/User.json#/some/schema/path/license" }).
 *
 * @param id The string representation of the id
 */
case class FragmentId(id: String) extends Id

/**
 * An implicit id marks the absense of an id. It implies that the schema should be uniquely identified by the schema
 * path (JSON path in the original JSON representation) from its nearest root schema towards itself. In other words,
 * an implicit id is a fragment id that hasn't been set.
 */
case object ImplicitId extends Id
