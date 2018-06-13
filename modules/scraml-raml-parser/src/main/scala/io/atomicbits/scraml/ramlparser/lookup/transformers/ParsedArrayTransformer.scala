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

package io.atomicbits.scraml.ramlparser.lookup.transformers

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator, ParsedToCanonicalTypeTransformer }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ ArrayType, ArrayTypeReference, CanonicalName, TypeReference }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedArray, ParsedType }

/**
  * Created by peter on 30/12/16.
  */
object ParsedArrayTransformer {

  // format: off
  def unapply(parsedTypeContext: ParsedTypeContext)
             (implicit canonicalNameGenerator: CanonicalNameGenerator): Option[(TypeReference, CanonicalLookupHelper)] = { // format: on

    val parsed: ParsedType                           = parsedTypeContext.parsedType
    val canonicalLookupHelper: CanonicalLookupHelper = parsedTypeContext.canonicalLookupHelper
    val canonicalNameOpt: Option[CanonicalName]      = parsedTypeContext.canonicalNameOpt
    val parentNameOpt: Option[CanonicalName]         = parsedTypeContext.parentNameOpt // This is the optional json-schema parent

    def registerParsedArray(parsedObject: ParsedArray): (TypeReference, CanonicalLookupHelper) = {

      val (genericTypeReference, canonicalLHUnused) = ParsedToCanonicalTypeTransformer.transform(parsedObject.items, canonicalLookupHelper)

      val typeReference: TypeReference = ArrayTypeReference(genericType = genericTypeReference)

      (typeReference, canonicalLookupHelper)
    }

    parsed match {
      case parsedArray: ParsedArray => Some(registerParsedArray(parsedArray))
      case _                        => None
    }
  }

}
