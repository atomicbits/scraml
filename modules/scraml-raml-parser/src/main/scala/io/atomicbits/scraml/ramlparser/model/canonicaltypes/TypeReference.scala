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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 9/12/16.
  */
trait TypeReference extends GenericReferrable {

  def refers: CanonicalName

  def genericTypes: List[GenericReferrable]

  def genericTypeParameters: List[TypeParameter]

}

case class NonPrimitiveTypeReference(refers: CanonicalName,
                                     genericTypes: List[GenericReferrable]      = List.empty,
                                     genericTypeParameters: List[TypeParameter] = List.empty)
    extends TypeReference

case class ArrayTypeReference(genericType: GenericReferrable) extends TypeReference {

  val refers = ArrayType.canonicalName

  val genericTypes: List[GenericReferrable] = List(genericType)

  val genericTypeParameters: List[TypeParameter] = List(TypeParameter("T"))

}

object TypeReference {

  def apply(ttype: CanonicalType): TypeReference = ttype match {
    case primitive: PrimitiveType       => primitive
    case nonPrimitive: NonPrimitiveType => NonPrimitiveTypeReference(nonPrimitive.canonicalName)
    case arrayType: ArrayType.type =>
      sys.error(s"Cannot create a type reference from an array type without knowing the type of elements it contains.")
  }

}
