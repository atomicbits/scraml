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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 25/03/16.
  */
/**
  * The base class for all Id's.
  */
sealed trait Id

/**
  * UniqueId's are Id's that are expected to be unique by value within a RAML document.
  */
sealed trait UniqueId extends Id

trait AbsoluteId extends UniqueId {

  def id: String

  def rootPart: RootId

  def rootPath: List[String]

  def hostPath: List[String]

  def fragments: List[String] = List.empty

}

/**
  * An absolute id uniquely identifies a schema. A schema with an absolute id is the root for its child-schemas that
  * don't have an absolute or relative id.
  * An absolute id is of the form "http://atomicbits.io/schema/User.json" and often it ends with a "#".
  *
  *
  */
case class RootId(hostPath: List[String], path: List[String], name: String) extends AbsoluteId {

  val anchor: String = s"http://${hostPath.mkString(".")}/${path.mkString("/")}"

  def toAbsoluteId(id: Id, path: List[String] = List.empty): AbsoluteId = {
    id match {
      case absoluteId: RootId                => absoluteId
      case relativeId: RelativeId            => RootId(s"$anchor/${relativeId.id}")
      case fragmentId: FragmentId            => AbsoluteFragmentId(this, fragmentId.fragments)
      case absFragmentId: AbsoluteFragmentId => absFragmentId
      case ImplicitId                        => AbsoluteFragmentId(this, path)
      case nativeId: NativeId                =>
        // We should not expect native ids inside a json-schema, but our parser doesn't separate json-schema and RAML 1.0 types,
        // so we can get fragments that are interpreted as having a native ID. This is OK, but we need to resolve them here
        // by using the NoId.
        NoId
      case absId: AbsoluteId => sys.error("All absolute IDs should be covered already.")
      case other             => sys.error(s"Cannot transform $other to an absolute id.")
    }
  }

  val rootPart: RootId = this

  val fileName: String = s"$name.json"

  val rootPath: List[String] = path :+ fileName

  val id = s"$anchor/$fileName"

}

object RootId {

  /**
    * @param id The string representation of the id
    */
  def apply(id: String): RootId = {

    val protocolParts   = id.split("://")
    val protocol        = protocolParts.head
    val withoutProtocol = protocolParts.takeRight(1).head
    val parts           = withoutProtocol.split('/').filter(_.nonEmpty)
    val host            = parts.take(1).head
    val hostPath        = host.split('.').toList
    val name            = fileNameToName(parts.takeRight(1).head)
    val path            = parts.drop(1).dropRight(1).toList

    RootId(
      hostPath = hostPath,
      path     = path,
      name     = name
    )
  }

  def fileNameToName(fileNameRep: String): String = {
    val filenameWithoutHashtags = fileNameRep.split('#').take(1).head
    val name                    = filenameWithoutHashtags.split('.').take(1).head
    name
  }

  def fromPackagePath(packagePath: List[String], name: String): RootId = {
    packagePath match {
      case p1 :: p2 :: ps => RootId(hostPath = List(p2, p1), path = ps, name = name)
      case _              => sys.error(s"A package path must contain at least two elements: $packagePath")
    }
  }

  def fromCanonical(canonicalName: CanonicalName): RootId = {

    val domain = canonicalName.packagePath.take(2).reverse.mkString(".")
    val path = canonicalName.packagePath.drop(2) match {
      case Nil      => "/"
      case somePath => somePath.mkString("/", "/", "/")
    }

    RootId(s"http://$domain$path${canonicalName.name}.json")
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
case class RelativeId(id: String) extends Id {

  val name: String = RootId.fileNameToName(id.split('/').filter(_.nonEmpty).takeRight(1).head)

  val path: List[String] = id.split('/').filter(_.nonEmpty).dropRight(1).toList

}

/**
  * A native id is like a relative id, but it is not expected to have an absolute parent id. NativeId's should not be used in
  * json-schema definitions. They have been added to cope with the native RAML 1.0 types that either have an NativeId or an ImplicitId.
  *
  * We cannot use the RootId concept here, because a NativeID has a free format whereas the RootId is a json-schema concept that
  * has to meet strict formatting rules.
  */
case class NativeId(id: String) extends UniqueId

/**
  * A fragment id identifies its schema uniquely by the schema path (JSON path in the original JSON representation)
  * from its nearest root schema towards itself. In other words, the fragment id should always match this schema
  * path and is redundant from that point of view.
  * It is of the form "#/some/schema/path/license"
  *
  * @param fragments The path that composes the fragment id.
  */
case class FragmentId(fragments: List[String]) extends Id {

  def id: String = s"#/${fragments.mkString("/")}"

}

/**
  * This is the absolute version of a fragment id. It is prepended with its root's achor.
  * E.g. "http://atomicbits.io/schema/User.json#/some/schema/path/license"
  *
  * @param root      The root of this absolute fragment id.
  * @param fragments The path that composes the fragment id.
  */
case class AbsoluteFragmentId(root: RootId, override val fragments: List[String]) extends AbsoluteId {

  def id: String = s"${root.id}#/${fragments.mkString("/")}"

  val rootPart: RootId = root

  val rootPath = root.rootPath

  val hostPath = root.hostPath

}

/**
  * An implicit id marks the absense of an id. It implies that the schema should be uniquely identified by the schema
  * path (JSON path in the original JSON representation) from its nearest root schema towards itself. In other words,
  * an implicit id is a fragment id that hasn't been set.
  *
  * It is not a UniqueId since may items can have ImplicitId's.
  */
case object ImplicitId extends Id

/**
  * Placeholder object for an ID that points to nowhere.
  */
case object NoId extends AbsoluteId {

  override def id: String = "http://no.where"

  override def rootPart: RootId = RootId(id)

  override def rootPath: List[String] = List.empty

  override def hostPath: List[String] = List("no", "where")

}
