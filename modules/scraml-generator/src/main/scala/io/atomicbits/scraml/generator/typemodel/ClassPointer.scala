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

package io.atomicbits.scraml.generator.typemodel

import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 10/01/17.
  */
sealed trait ClassPointer

trait PrimitiveClassPointer extends ClassPointer

case class ClassReference(name: String,
                          packageParts: List[String]          = List.empty,
                          typeParameters: List[TypeParameter] = List.empty,
                          typeParamValues: List[ClassPointer] = List.empty,
                          arrayType: Option[ClassReference]   = None,
                          predef: Boolean                     = false,
                          library: Boolean                    = false,
                          isTypeParameter: Boolean            = false)
    extends ClassPointer {

  lazy val canonicalName: CanonicalName = CanonicalName.create(name, packageParts)

  /**
    * The base form for this class reference. The base form refers to the class in its most unique way,
    * without type parameter values.
    * e.g. List[T] and not List[Dog]
    */
  lazy val base: ClassReference = if (typeParamValues.isEmpty) this else copy(typeParamValues = List.empty)

  val isArray: Boolean = arrayType.isDefined

}

case object StringClassPointer extends PrimitiveClassPointer

case object ByteClassPointer extends PrimitiveClassPointer

case class LongClassPointer(primitive: Boolean = true) extends PrimitiveClassPointer

case class DoubleClassPointer(primitive: Boolean = true) extends PrimitiveClassPointer

case class BooleanClassPointer(primitive: Boolean = true) extends PrimitiveClassPointer

case class TypeParameter(name: String) extends ClassPointer

case class ArrayClassPointer(arrayType: ClassPointer) extends ClassPointer

case object BinaryDataClassPointer extends ClassPointer

case object InputStreamClassPointer extends ClassPointer

case object FileClassPointer extends ClassPointer

case object JsObjectClassPointer extends ClassPointer

case object JsValueClassPointer extends ClassPointer

case class ListClassPointer(typeParamValue: ClassPointer) extends ClassPointer

case object BodyPartClassPointer extends ClassPointer
