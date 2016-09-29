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

package io.atomicbits.scraml.generator.lookup

import io.atomicbits.scraml.generator._
import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.types._


/**
  * A lookup table to follow schema ids and external links to schema definitions (JsObject) and canonical names.
  *
  * @param lookupTable Maps absolute schema ids (and relative schema ids after they have been expanded to their
  *                    absolute form) to the schema definition. Mind that not all schema definitions represent
  *                    object types, they can represent any type, or even no type (usually when defining nested
  *                    schemas).
  * @param nativeIdMap Maps the external schema links to the corresponding schema id. That schema id then
  *                    corresponds with a schema in the lookupTable. That schema should represent an
  *                    actual type (integer, number, string, boolean, object, List[integer], List[number],
  *                    List[string], List[boolean], List[object], or even nested lists).
  */
case class TypeLookupTable(lookupTable: Map[AbsoluteId, Type] = Map.empty,
                           nativeIdMap: Map[NativeId, AbsoluteId] = Map.empty,
                           objectMap: Map[AbsoluteId, ObjectModel] = Map.empty,
                           enumMap: Map[AbsoluteId, EnumType] = Map.empty,
                           classReps: Map[AbsoluteId, ClassRep] = Map.empty,
                           nativeToRootId: NativeId => RootId) {

  def map(f: TypeLookupTable => TypeLookupTable): TypeLookupTable = f(this)


  def lookup(id: Id): Type = {


    def fragmentSearch(ttype: Type, fragmentPath: List[String]): Type = {
      fragmentPath match {
        case Nil       => ttype
        case fr :: frs =>
          ttype match {
            case fragmentedSchema: Fragmented =>
              fragmentedSchema.fragments.fragmentMap.get(fr)
                .map(fragmentSearch(_, frs))
                .getOrElse(sys.error(s"Cannot follow fragment $fr into ${fragmentedSchema.id}"))
            case _                           => sys.error(s"Cannot follow the following fragment path: $id")
          }
      }
    }

    id match {
      case absoluteId: AbsoluteId => fragmentSearch(lookupTable(absoluteId.rootPart), absoluteId.fragments)
      case nativeId: NativeId     =>
        val actualNativeId = nativeIdMap.getOrElse(nativeId, sys.error(s"Cannot find type with native id $id."))
        lookupTable.getOrElse(actualNativeId, sys.error(s"Cannot find type with native id $id."))
      case otherId                =>
        sys.error(s"Cannot lookup $otherId in the typeLookupTable. Only absolute IDs and native IDs can be requested.")
    }

  }

}
