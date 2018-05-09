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

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 9/12/16.
  */
trait TypeConstraints[T <: GenericReferrable]

case class StringConstraints(format: Option[String]  = None,
                             pattern: Option[String] = None,
                             minLength: Option[Int]  = None,
                             maxLength: Option[Int]  = None)
    extends TypeConstraints[StringType.type]

case class FileConstraints(fileTypes: Option[Seq[String]] = None, minLength: Option[Int] = None, maxLength: Option[Int] = None)
    extends TypeConstraints[FileType.type]

case class IntegerConstraints(format: Option[String]  = None,
                              minimum: Option[Int]    = None,
                              maximum: Option[Int]    = None,
                              multipleOf: Option[Int] = None)
    extends TypeConstraints[IntegerType.type]

case class NumberConstraints(format: Option[String]  = None,
                             minimum: Option[Int]    = None,
                             maximum: Option[Int]    = None,
                             multipleOf: Option[Int] = None)
    extends TypeConstraints[NumberType.type]
