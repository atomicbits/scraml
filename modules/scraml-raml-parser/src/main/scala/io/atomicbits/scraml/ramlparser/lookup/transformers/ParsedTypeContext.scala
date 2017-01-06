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
                             canonicalNameOpt: Option[CanonicalName] = None,
                             parentNameOpt: Option[CanonicalName] = None,
                             imposedTypeDiscriminator: Option[String] = None)
