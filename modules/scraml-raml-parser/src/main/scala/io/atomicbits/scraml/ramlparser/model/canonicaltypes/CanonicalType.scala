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
