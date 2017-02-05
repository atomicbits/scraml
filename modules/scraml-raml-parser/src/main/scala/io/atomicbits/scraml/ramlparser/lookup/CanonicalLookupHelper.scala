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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, CanonicalType, NonPrimitiveType }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ Fragmented, ParsedNull, ParsedType, Types }

/**
  * Created by peter on 17/12/16.
  */
/**
  *
  * @param parsedTypeIndex                 The types that are explicitely declared the in the Raml model's types declaration and
  *                                        those that are implicitely declared inside the resource's parameters (e.g. GET parameters)
  *                                        and body content of responses and replies. Each type declaration is keyed on its
  *                                        native (RAML) id.
  * @param referenceOnlyParsedTypeIndex    Like parsedTypeIndex, but the elements in this index will not be used directly to create
  *                                        canonical types later on. This index will only contain the Selection elements of a json-schema
  *                                        object. We need to be able to look up those elements, but we will generate their canonical
  *                                        form through their parent.
  * @param lookupTable                     The canonical lookup map.
  * @param jsonSchemaNativeToAbsoluteIdMap Native references may be used in the RAML definition to refer to json-schema types that
  *                                        have their own json-schema id internally. This map enables us to translate the canonical
  *                                        name that matches the native id to the canonical name that matches the json-schema id.
  */
case class CanonicalLookupHelper(lookupTable: Map[CanonicalName, NonPrimitiveType]          = Map.empty,
                                 parsedTypeIndex: Map[UniqueId, ParsedType]                 = Map.empty,
                                 referenceOnlyParsedTypeIndex: Map[UniqueId, ParsedType]    = Map.empty,
                                 jsonSchemaNativeToAbsoluteIdMap: Map[NativeId, AbsoluteId] = Map.empty) {

  def getParsedType(id: Id): Option[ParsedType] = {
    id match {
      case NoId => Some(ParsedNull())
      case nativeId: NativeId =>
        val realIndex = jsonSchemaNativeToAbsoluteIdMap.getOrElse(nativeId, nativeId)
        List(parsedTypeIndex.get(realIndex), referenceOnlyParsedTypeIndex.get(realIndex)).flatten.headOption
      case uniqueId: UniqueId =>
        List(parsedTypeIndex.get(uniqueId), referenceOnlyParsedTypeIndex.get(uniqueId)).flatten.headOption
      case other => None
    }
  }

  def addCanonicalType(canonicalName: CanonicalName, canonicalType: NonPrimitiveType): CanonicalLookupHelper =
    copy(lookupTable = lookupTable + (canonicalName -> canonicalType))

  def addParsedTypeIndex(id: Id, parsedType: ParsedType, lookupOnly: Boolean = false): CanonicalLookupHelper = {
    id match {
      case uniqueId: UniqueId =>
        if (lookupOnly) {
          copy(referenceOnlyParsedTypeIndex = referenceOnlyParsedTypeIndex + (uniqueId -> parsedType))
        } else {
          copy(parsedTypeIndex = parsedTypeIndex + (uniqueId -> parsedType))
        }
      case _ => this
    }
  }

  def addJsonSchemaNativeToAbsoluteIdTranslation(jsonSchemaNativeId: NativeId, jsonSchemaAbsoluteId: AbsoluteId) =
    copy(jsonSchemaNativeToAbsoluteIdMap = jsonSchemaNativeToAbsoluteIdMap + (jsonSchemaNativeId -> jsonSchemaAbsoluteId))

}
