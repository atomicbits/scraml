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

import io.atomicbits.scraml.ramlparser.lookup.CanonicalLookupHelper
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedType

/**
  * Created by peter on 23/12/16.
  */
/**
  * Helper class that contains information for transforming a specific parsed type into its canonical counterpart.
  *
  * @param parsedType The parsed type which is the subject for this context.
  * @param canonicalLookupHelper The canonical lookup helper containing all collected type information.
  * @param canonicalNameOpt The optional canonical name for the given parsedType.
  * @param parentNameOpt The optional canonical name for the parent of the parsedType. This variable is only present in some cases
  *                      for json-schema type definitions.
  * @param imposedTypeDiscriminator The optional type discriminator property name that was specified by a parent class
  */
case class ParsedTypeContext(parsedType: ParsedType,
                             canonicalLookupHelper: CanonicalLookupHelper,
                             canonicalNameOpt: Option[CanonicalName]  = None,
                             parentNameOpt: Option[CanonicalName]     = None,
                             imposedTypeDiscriminator: Option[String] = None)
