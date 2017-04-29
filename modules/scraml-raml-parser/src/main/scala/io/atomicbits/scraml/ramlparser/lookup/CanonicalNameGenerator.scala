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

package io.atomicbits.scraml.ramlparser.lookup

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 17/12/16.
  */
case class CanonicalNameGenerator(defaultBasePath: List[String]) {

  def generate(id: Id): CanonicalName = id match {
    case absoluteId: AbsoluteId => absoluteIdToCanonicalName(absoluteId)
    case relId: RelativeId      => CanonicalName.create(name = relId.name, packagePath = defaultBasePath ++ relId.path)
    case nativeId: NativeId     => CanonicalName.create(name = nativeId.id, packagePath = defaultBasePath)
    case ImplicitId             => CanonicalName.noName(packagePath = defaultBasePath)
    case x                      => sys.error(s"Cannot create a canonical name from a ${x.getClass.getSimpleName}")
  }

  def toRootId(id: Id): RootId = RootId.fromCanonical(generate(id))

  private def absoluteIdToCanonicalName(origin: AbsoluteId): CanonicalName = {

    val hostPathReversed = origin.hostPath.reverse
    val relativePath     = origin.rootPath.dropRight(1)
    val originalFileName = origin.rootPath.takeRight(1).head
    val fragmentPath     = origin.fragments

    // E.g. when the origin is: http://atomicbits.io/api/schemas/myschema.json#/definitions/schema2
    // then:
    // hostPathReversed = List("io", "atomicbits")
    // relativePath = List("api", "schemas")
    // originalFileName = "myschema.json"
    // fragmentPath = List("definitions", "schema2")

    val classBaseName = CanonicalName.cleanClassNameFromFileName(originalFileName)
    val path          = hostPathReversed ++ relativePath
    val fragment      = fragmentPath

    val className = fragment.foldLeft(classBaseName) { (classNm, fragmentPart) =>
      s"$classNm${CanonicalName.cleanClassName(fragmentPart)}"
    }

    CanonicalName.create(name = className, packagePath = path)
  }

}
