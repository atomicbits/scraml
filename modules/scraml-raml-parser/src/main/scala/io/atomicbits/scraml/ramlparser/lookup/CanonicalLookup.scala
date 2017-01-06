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

import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, CanonicalType }

/**
  * Created by peter on 17/12/16.
  */
case class CanonicalLookup(map: Map[CanonicalName, CanonicalType] = Map.empty) {

  def apply(canonicalName: CanonicalName): CanonicalType = map(canonicalName)

  def get(canonicalName: CanonicalName): Option[CanonicalType] = map.get(canonicalName)

}
