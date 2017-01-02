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

package io.atomicbits.scraml.ramlparser.lookup.transformers

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator }
import io.atomicbits.scraml.ramlparser.model.{ AbsoluteId, ImplicitId, NativeId }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, NonPrimitiveTypeReference, TypeReference }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedType, ParsedTypeReference }

/**
  * Created by peter on 23/12/16.
  */
object ParsedTypeReferenceTransformer {

  // format: off
  def unapply(parsedTypeContext: ParsedTypeContext)
             (implicit canonicalNameGenerator: CanonicalNameGenerator): Option[(TypeReference, CanonicalLookupHelper)] = { // format: on

    val parsed: ParsedType                           = parsedTypeContext.parsedType
    val canonicalLookupHelper: CanonicalLookupHelper = parsedTypeContext.canonicalLookupHelper
    val canonicalNameOpt: Option[CanonicalName]      = parsedTypeContext.canonicalNameOpt
    val parentNameOpt: Option[CanonicalName]         = parsedTypeContext.parentNameOpt

    // If this is a type reference to a (fragmented) json-schema type, then we need to register it as such with the canonicalLookupHelper.
    // In this case, we also need to use the parsedTypeContext.canonicalLookupHelper.declaredTypes map to look up the canonical name
    // as defined by the json-schema id (and not the native id of the RAML definition).

    def registerParsedTypeReference(parsedTypeReference: ParsedTypeReference): (TypeReference, CanonicalLookupHelper) = {
      parsedTypeReference.refersTo match {
        case absoluteId: AbsoluteId =>
          val jsonSchemaCanonicalName = canonicalNameGenerator.generate(absoluteId)
          val typeReference           = NonPrimitiveTypeReference(jsonSchemaCanonicalName)
          // Can we assume that each type reference is a NonPrimitiveTypeReference?
          // How do we capture forward references to json-schema types that are defined inline somewhere inside the resource definitions?
          (typeReference, canonicalLookupHelper)
        case nativeId: NativeId =>
          val generatedCanonicalName =
            canonicalLookupHelper.parsedTypeIndex.get(nativeId).flatMap { parsedType =>
              parsedType.id match {
                case absoluteId: AbsoluteId => Some(canonicalNameGenerator.generate(absoluteId))
                case _                      => None
              }
            } getOrElse canonicalNameGenerator.generate(nativeId)

          val typeReference = NonPrimitiveTypeReference(generatedCanonicalName)
          (typeReference, canonicalLookupHelper)
        case unexpected => sys.error(s"Unexpected id in the types definition: $unexpected")
      }
    }

    parsed match {
      case parsedTypeReference: ParsedTypeReference => Some(registerParsedTypeReference(parsedTypeReference))
      case _                                        => None
    }
  }

}
