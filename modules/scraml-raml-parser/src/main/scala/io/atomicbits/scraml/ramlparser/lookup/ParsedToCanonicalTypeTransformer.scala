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

import org.slf4j.{ Logger, LoggerFactory }
import io.atomicbits.scraml.ramlparser.lookup.transformers._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 17/12/16.
  */
object ParsedToCanonicalTypeTransformer {

  val LOGGER: Logger = LoggerFactory.getLogger("ParsedToCanonicalTypeTransformer")

  /**
    * Transform the given parsed type to a canonical type, recursively parsing and registering all internal parsed types .
    *
    * @param parsed                The parsed type to transform.
    * @param canonicalLookupHelper This canonical lookup helper registers all internal parsed types that we see while processing the
    *                              given parsed type.
    * @return
    */
  // format: off
  def transform(parsed: ParsedType,
                canonicalLookupHelper: CanonicalLookupHelper,
                canonicalName: Option[CanonicalName] = None)
               (implicit canonicalNameGenerator: CanonicalNameGenerator): (GenericReferrable, CanonicalLookupHelper) = { // format: on

    ParsedTypeContext(parsed, canonicalLookupHelper, canonicalName) match {
      case ParsedObjectTransformer(typeReference, updatedLookupHelper)        => (typeReference, updatedLookupHelper)
      case ParsedTypeReferenceTransformer(typeReference, updatedLookupHelper) => (typeReference, updatedLookupHelper)
      case ParsedEnumTransformer(typeReference, updatedLookupHelper)          => (typeReference, updatedLookupHelper)
      case ParsedArrayTransformer(typeReference, updatedLookupHelper)         => (typeReference, updatedLookupHelper)
      case ParsedBooleanTransformer(typeReference, updatedLookupHelper)       => (typeReference, updatedLookupHelper)
      case ParsedStringTransformer(typeReference, updatedLookupHelper)        => (typeReference, updatedLookupHelper)
      case ParsedNumberTransformer(typeReference, updatedLookupHelper)        => (typeReference, updatedLookupHelper)
      case ParsedIntegerTransformer(typeReference, updatedLookupHelper)       => (typeReference, updatedLookupHelper)
      case ParsedNullTransformer(typeReference, updatedLookupHelper)          => (typeReference, updatedLookupHelper)
      case ParsedGenericObjectTransformer(typeReference, updatedLookupHelper) => (typeReference, updatedLookupHelper)
      case ParsedDateTransformer(typeReference, updatedLookupHelper)          => (typeReference, updatedLookupHelper)
      case ParsedTypeContext(fragments: Fragments, _, _, _, _) =>
        LOGGER.info(s"Skipped unknown json-schema fragments.")
        (NullType, canonicalLookupHelper)
      case FallbackTransformer(typeReference, updatedLookupHelper) =>
        println(s"WARNING: type ${parsed.getClass.getSimpleName} is not yet supported, we use 'string' as the fallback type for now.")
        (typeReference, updatedLookupHelper)
      // Currently not yet supported:
      //      case parsedFile: ParsedFile                               => ???
      //      case parsedNull: ParsedNull                               => ???
      //      case parsedUnionType: ParsedUnionType                     => ???
      case x => sys.error(s"Error transforming $x")
    }

  }

}
