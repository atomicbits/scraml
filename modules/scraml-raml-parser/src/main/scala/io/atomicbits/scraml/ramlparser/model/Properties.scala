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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.Type

/**
  * Created by peter on 4/12/16.
  */
case class Properties(valueMap: Map[String, Property] = Map.empty) {

  def apply(name: String): Property = valueMap(name)

  def get(name: String): Option[Property] = valueMap.get(name)

  def map(f: Property => Property): Properties = {
    copy(valueMap = valueMap.mapValues(f))
  }

  def asTypeMap: Map[String, Type] = {
    valueMap.mapValues(_.propertyType)
  }

  val values: List[Property] = valueMap.values.toList

  val types: List[Type] = valueMap.values.map(_.propertyType).toList

  val isEmpty = valueMap.isEmpty

}


object Properties {

  def fromTypeMap(propertyMap: Map[String, Type]): Properties = {
    val properties =
      propertyMap.map {
        case (name, pType) =>
          val property =
            Property(
              name = name,
              propertyType = pType,
              required = pType.isRequired
            )
          (name, property)
      }
    Properties(valueMap = properties)
  }

}
