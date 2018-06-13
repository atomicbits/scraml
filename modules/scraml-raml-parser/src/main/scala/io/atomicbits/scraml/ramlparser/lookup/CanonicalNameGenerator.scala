/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
