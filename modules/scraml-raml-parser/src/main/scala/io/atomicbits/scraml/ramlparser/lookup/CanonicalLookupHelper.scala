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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, NonPrimitiveType }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedNull, ParsedType }
import org.slf4j.{ Logger, LoggerFactory }

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

  val logger: Logger = LoggerFactory.getLogger(CanonicalLookupHelper.getClass)

  def getParsedTypeWithProperId(id: Id): Option[ParsedType] = {
    val parsedTypeOpt =
      id match {
        case NoId =>
          Some(ParsedNull())
        case nativeId: NativeId =>
          val realIndex = jsonSchemaNativeToAbsoluteIdMap.getOrElse(nativeId, nativeId)
          List(parsedTypeIndex.get(realIndex), referenceOnlyParsedTypeIndex.get(realIndex)).flatten.headOption
        case uniqueId: UniqueId =>
          List(parsedTypeIndex.get(uniqueId), referenceOnlyParsedTypeIndex.get(uniqueId)).flatten.headOption
        case other => None
      }
    parsedTypeOpt.map { parsedType =>
      parsedType.id match {
        case ImplicitId => parsedType.updated(id)
        case other      => parsedType
      }
    }
  }

  def addCanonicalType(canonicalName: CanonicalName, canonicalType: NonPrimitiveType): CanonicalLookupHelper =
    copy(lookupTable = lookupTable + (canonicalName -> canonicalType))

  def addParsedTypeIndex(id: Id, parsedType: ParsedType, lookupOnly: Boolean = false): CanonicalLookupHelper = {

    def warnDuplicate(uniqueId: UniqueId, parsedT: ParsedType): Unit = {
      parsedTypeIndex.get(uniqueId).collect {
        case pType if pType != parsedT => logger.debug(s"Duplicate type definition found for id $uniqueId")
      }
      ()
    }

    id match {
      case uniqueId: UniqueId =>
        if (lookupOnly) {
          copy(referenceOnlyParsedTypeIndex = referenceOnlyParsedTypeIndex + (uniqueId -> parsedType))
        } else {
          warnDuplicate(uniqueId, parsedType)
          copy(parsedTypeIndex = parsedTypeIndex + (uniqueId -> parsedType))
        }
      case _ => this
    }
  }

  def addJsonSchemaNativeToAbsoluteIdTranslation(jsonSchemaNativeId: NativeId, jsonSchemaAbsoluteId: AbsoluteId): CanonicalLookupHelper =
    copy(jsonSchemaNativeToAbsoluteIdMap = jsonSchemaNativeToAbsoluteIdMap + (jsonSchemaNativeId -> jsonSchemaAbsoluteId))

}
