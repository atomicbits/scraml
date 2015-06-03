package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
sealed trait IdType

case class Root(id: String) extends IdType {

  lazy val anchor: String = id.split('/').toList.dropRight(1).mkString("/")

  def rootFromRelative(relative: Relative) = Root(s"$anchor/${relative.id}")
  
  def rootFromFragment(fragment: Fragment) = Root(s"$id${fragment.id}")

  def expandRef(ref: String): String = {

    val refTrimmed = ref.trim

    if (refTrimmed.contains("://")) refTrimmed // Absolute
    else if (refTrimmed.startsWith("#")) s"$id$refTrimmed" // Fragment
    else s"$anchor/$refTrimmed" // Relative

  }

}

case class Relative(id: String) extends IdType

case class Fragment(id: String) extends IdType

case object NoId extends IdType
