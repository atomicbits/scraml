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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 9/12/16.
  */
case class ObjectType(canonicalName: CanonicalName,
                      properties: Map[String, Property[_ <: GenericReferrable]],
                      parents: List[TypeReference]           = List.empty,
                      typeParameters: List[TypeParameter]    = List.empty,
                      typeDiscriminator: Option[String]      = None,
                      typeDiscriminatorValue: Option[String] = None)
    extends NonPrimitiveType
