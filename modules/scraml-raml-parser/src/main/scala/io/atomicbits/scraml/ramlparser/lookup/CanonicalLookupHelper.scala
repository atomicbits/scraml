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

import io.atomicbits.scraml.ramlparser.model.{ AbsoluteFragmentId, AbsoluteId, Id, NativeId }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, CanonicalType }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ Fragmented, ParsedType, Types }

/**
  * Created by peter on 17/12/16.
  */
/**
  *
  * @param parsedTypeIndex                 The types that are explicitely declared the in the Raml model's types declaration and
  *                                        those that are implicitely declared inside the resource's parameters (e.g. GET parameters)
  *                                        and body content of responses and replies. Each type declaration is keyed on its
  *                                        native (RAML) id.
  * @param lookupTable                     The canonical lookup map.
  * @param jsonSchemaLookupTable           A lookup table where all json-schema parsed schemas are collected so that we can search for
  *                                        references later on. Because of the way json-schema can have forward references to other
  *                                        json-schema's, we have to build up this jsonSchemaLookupTable before we start transforming
  *                                        the parsed types to their canonical model.
  * @param jsonSchemaNativeToAbsoluteIdMap Native references may be used in the RAML definition to refer to json-schema types that
  *                                        have their own json-schema id internally. This map enables us to translate the canonical
  *                                        name that matches the native id to the canonical name that matches the json-schema id.
  * @param jsonSchemaFragmentReferences    A list that is used to track all known fragment references to types. These are useful to
  *                                        search for types in json-schema fragments when deducing the canonical type references.
  */
case class CanonicalLookupHelper(lookupTable: Map[CanonicalName, CanonicalType]                       = Map.empty,
                                 parsedTypeIndex: Map[Id, ParsedType]                                 = Map.empty,
                                 jsonSchemaLookupTable: Map[AbsoluteId, ParsedType]                   = Map.empty,
                                 jsonSchemaNativeToAbsoluteIdMap: Map[NativeId, AbsoluteId]           = Map.empty,
                                 jsonSchemaFragmentReferences: Map[AbsoluteFragmentId, CanonicalName] = Map.empty) {
  //                             jsonSchemaNativeToAbsoluteIdMap: Map[CanonicalName, CanonicalName]   = Map.empty,

  def addCanonicalType(canonicalName: CanonicalName, canonicalType: CanonicalType): CanonicalLookupHelper =
    copy(lookupTable = lookupTable + (canonicalName -> canonicalType))

  def addParsedTypeIndex(id: Id, parsedType: ParsedType): CanonicalLookupHelper =
    copy(parsedTypeIndex = parsedTypeIndex + (id -> parsedType))

  def addJsonSchemaType(absoluteId: AbsoluteId, parsedType: ParsedType): CanonicalLookupHelper =
    copy(jsonSchemaLookupTable = jsonSchemaLookupTable + (absoluteId -> parsedType))

  def addJsonSchemaNativeToAbsoluteIdTranslation(jsonSchemaNativeId: NativeId, jsonSchemaAbsoluteId: AbsoluteId) =
    copy(jsonSchemaNativeToAbsoluteIdMap = jsonSchemaNativeToAbsoluteIdMap + (jsonSchemaNativeId -> jsonSchemaAbsoluteId))

  def addFragmentReference(absoluteFragmentId: AbsoluteFragmentId, canonicalName: CanonicalName): CanonicalLookupHelper =
    copy(jsonSchemaFragmentReferences = jsonSchemaFragmentReferences + (absoluteFragmentId -> canonicalName))

  def lookupJsonSchemaType(absoluteId: AbsoluteId): ParsedType = {

    def fragmentSearch(ttype: ParsedType, fragmentPath: List[String]): ParsedType = {
      fragmentPath match {
        case Nil => ttype
        case fr :: frs =>
          ttype match {
            case fragmentedSchema: Fragmented =>
              fragmentedSchema.fragments.fragmentMap
                .get(fr)
                .map(fragmentSearch(_, frs))
                .getOrElse(sys.error(s"Cannot follow fragment $fr into ${fragmentedSchema.id}"))
            case _ => sys.error(s"Cannot follow the following fragment path: $absoluteId")
          }
      }
    }

    fragmentSearch(jsonSchemaLookupTable(absoluteId.rootPart), absoluteId.fragments)
  }

}
