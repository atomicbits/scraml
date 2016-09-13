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

package io.atomicbits.scraml.generator.lookup

import io.atomicbits.scraml.ramlparser.model._


/**
  * Created by peter on 22/07/15.
  */
object TypeUtils {

  def asUniqueId(id: Id): UniqueId = {

    id match {
      case uniqueId: UniqueId => uniqueId
      case _                  => sys.error(s"$id is not a unique id.")
    }

  }


  def asAbsoluteId(id: Id, nativeToRootId: Option[NativeId => RootId] = None): AbsoluteId = {

    id match {
      case nativeId: NativeId     =>
        nativeToRootId.collect {
          case fn => fn(nativeId)
        } getOrElse sys.error(s"$id is not a native id and no transformation function was given.")
      case absoluteId: AbsoluteId => absoluteId
    }

  }

}
