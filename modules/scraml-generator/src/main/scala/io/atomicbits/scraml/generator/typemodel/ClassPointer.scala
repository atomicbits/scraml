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

case object DateTimeRFC3339ClassPointer extends ClassPointer

case object DateTimeRFC2616ClassPointer extends ClassPointer

case object DateTimeOnlyClassPointer extends ClassPointer

case object TimeOnlyClassPointer extends ClassPointer

case object DateOnlyClassPointer extends ClassPointer
