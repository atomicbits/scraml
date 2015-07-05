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

import io.atomicbits.scraml.jsonschemaparser.Id
import io.atomicbits.scraml.jsonschemaparser.model.{ObjectEl, Selection, Schema}

/**
 * Created by peter on 5/07/15. 
 */
case class ObjectElExt(id: Id,
                       properties: Map[String, Schema],
                       required: Boolean,
                       requiredFields: List[String] = List.empty,
                       selection: Option[Selection] = None,
                       fragments: Map[String, Schema] = Map.empty,
                       name: Option[String] = None,
                       parent: Option[ObjectElExt] = None,
                       children: List[ObjectElExt] = List.empty,
                       typeDiscriminatorValue: Option[String] = None) {

  def hasChildren: Boolean = children.nonEmpty

  def hasParent: Boolean = parent.isDefined

}


object ObjectElExt {

  def apply(obj: ObjectEl): ObjectElExt =
    ObjectElExt(
      id = obj.id,
      properties = obj.properties,
      required = obj.required,
      requiredFields = obj.requiredFields,
      selection = obj.selection,
      fragments = obj.fragments,
      name = obj.name
    )

}
