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

package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */

/**
 * Base class for a schema id.
 * In a correctly shaped schema, all schema ids can be expanded to their root-form.
 */
sealed trait Id

trait AbsoluteId extends Id {

  def id: String

  def rootPart: RootId

  def rootPath: List[String]

  def fragments: List[String] = List.empty

}

/**
 * An absolute id uniquely identifies a schema. A schema with an absolute id is the root for its child-schemas that
 * don't have an absolute or relative id.
 * An absolute id is of the form "http://atomicbits.io/schema/User.json" and often it ends with a "#".
 *
 * @param id The string representation of the id
 */
case class RootId(id: String) extends AbsoluteId {

  lazy val anchor: String = id.split('/').toList.dropRight(1).mkString("/")

  def toAbsolute(id: Id, path: List[String] = List.empty): AbsoluteId = {
    id match {
      case absoluteId: RootId => absoluteId
      case relativeId: RelativeId => RootId(s"$anchor/${relativeId.id}")
      case fragmentId: FragmentId => AbsoluteFragmentId(this, fragmentId.fragments)
      case absFragmentId: AbsoluteFragmentId => absFragmentId
      case ImplicitId => AbsoluteFragmentId(this, path)
    }
  }

  override def rootPart: RootId = this

  override def rootPath: List[String] = {
    val withoutProtocol = id.split("://").drop(1).head
    val withoutHost = withoutProtocol.split("/").drop(1).toList
    withoutHost
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
 * @param fragments The path that composes the fragment id.
 */
case class FragmentId(fragments: List[String]) extends Id {

  def id: String = s"#/${fragments.mkString("/")}"

}

/**
 * This is the absolute version of a fragment id. It is prepended with its root's achor.
 * E.g. "http://atomicbits.io/schema/User.json#/some/schema/path/license"
 *
 * @param root The root of this absolute fragment id.
 * @param fragments The path that composes the fragment id.
 */
case class AbsoluteFragmentId(root: RootId, override val fragments: List[String]) extends AbsoluteId {

  def id: String = s"${root.id}#/${fragments.mkString("/")}"

  override def rootPart: RootId = root

  override def rootPath = rootPart.rootPath

}

/**
 * An implicit id marks the absense of an id. It implies that the schema should be uniquely identified by the schema
 * path (JSON path in the original JSON representation) from its nearest root schema towards itself. In other words,
 * an implicit id is a fragment id that hasn't been set.
 */
case object ImplicitId extends Id
