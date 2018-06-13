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

/**
  * Created by peter on 22/07/15.
  *
  * ToDo: move this functionality inside the TypeLookupTable to simplify its interface.
  */
object TypeUtils {

  def asUniqueId(id: Id): UniqueId = {

    id match {
      case uniqueId: UniqueId => uniqueId
      case _                  => sys.error(s"$id is not a unique id.")
    }

  }

  def asAbsoluteId(id: Id, nativeToRootId: NativeId => AbsoluteId): AbsoluteId = {

    id match {
      case nativeId: NativeId     => nativeToRootId(nativeId)
      case absoluteId: AbsoluteId => absoluteId
      case other                  => sys.error(s"Cannot transform $other into an absolute id.")
    }

  }

}
