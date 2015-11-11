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

package io.atomicbits.scraml.client.manual

import io.atomicbits.scraml.dsl.json.TypedJson._

import play.api.libs.json.{Format, Json}

sealed trait Animal {

  def gender: String

}

sealed trait Mammal extends Animal {

  def name: Option[String]

}

object Mammal {

  implicit val mammalFormat: Format[Mammal] =
    TypeHintFormat(
      "type",
      Cat.jsonFormatter.withTypeHint("Cat"),
      Dog.jsonFormatter.withTypeHint("Dog")
    )

}

case class Cat(gender: String, name: Option[String] = None) extends Mammal

object Cat {

  implicit val jsonFormatter: Format[Cat] = Json.format[Cat]

}


case class Dog(gender: String, name: Option[String] = None, canBark: Boolean = true) extends Mammal

object Dog {

  implicit val jsonFormatter: Format[Dog] = Json.format[Dog]

}

case class Fish(gender: String) extends Animal

object Fish {

  implicit val jsonFormatter: Format[Fish] = Json.format[Fish]

}
