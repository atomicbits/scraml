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
trait TypeConstraints[T <: TypeReference]


case class StringConstraints(format: Option[String] = None,
                             pattern: Option[String] = None,
                             minLength: Option[Int] = None,
                             maxLength: Option[Int] = None) extends TypeConstraints[StringType.type]


case class FileConstraints(fileTypes: Option[Seq[String]] = None,
                           minLength: Option[Int] = None,
                           maxLength: Option[Int] = None) extends TypeConstraints[FileType.type]


case class IntegerConstraints(format: Option[String] = None,
                              minimum: Option[Int] = None,
                              maximum: Option[Int] = None,
                              multipleOf: Option[Int] = None) extends TypeConstraints[IntegerType.type ]


case class NumberConstraints(format: Option[String] = None,
                             minimum: Option[Int] = None,
                             maximum: Option[Int] = None,
                             multipleOf: Option[Int] = None) extends TypeConstraints[NumberType.type]

