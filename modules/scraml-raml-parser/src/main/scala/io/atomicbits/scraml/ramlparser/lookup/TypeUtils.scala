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
