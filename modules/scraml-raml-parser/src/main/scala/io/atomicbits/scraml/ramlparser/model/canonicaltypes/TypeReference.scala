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
trait TypeReference extends GenericReferrable {

  def refers: CanonicalName

  def genericTypes: Map[TypeParameter, GenericReferrable]

}

case class NonPrimitiveTypeReference(refers: CanonicalName, genericTypes: Map[TypeParameter, GenericReferrable] = Map.empty)
    extends TypeReference

case class ArrayTypeReference(genericType: GenericReferrable) extends TypeReference {

  val refers = ArrayType.canonicalName

  val genericTypes: Map[TypeParameter, GenericReferrable] = Map(ArrayType.typeParameter -> genericType)

}

object TypeReference {

  def apply(ttype: CanonicalType): TypeReference = ttype match {
    case primitive: PrimitiveType       => primitive
    case nonPrimitive: NonPrimitiveType => NonPrimitiveTypeReference(nonPrimitive.canonicalName)
    case arrayType: ArrayType.type =>
      sys.error(s"Cannot create a type reference from an array type without knowing the type of elements it contains.")
  }

}
