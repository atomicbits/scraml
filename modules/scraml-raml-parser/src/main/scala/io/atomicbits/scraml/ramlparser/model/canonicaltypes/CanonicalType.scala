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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 9/12/16.
  */
trait CanonicalType {

  def canonicalName: CanonicalName

//  def typeReference: TypeReference

}

/**
  * A primitive type is interpreted in the broad sense of all types that are not customly made by the user in the RAML model.
  * These are all types that are not an Object or an Enum.
  *
  * A primitive type is also its own type reference since it has no user-defined properties that can be configured in the RAML model.
  */
trait PrimitiveType extends CanonicalType with TypeReference {

  def genericTypes: List[TypeReference] = List.empty

  def genericTypeParameters: List[TypeParameter] = List.empty

}

/**
  * Nonprimitive types are the custom types created by the RAML model that will need to be generated as 'new' types.
  */
trait NonPrimitiveType extends CanonicalType
