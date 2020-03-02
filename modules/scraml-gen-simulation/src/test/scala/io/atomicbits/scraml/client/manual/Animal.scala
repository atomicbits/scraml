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

package io.atomicbits.scraml.client.manual

import io.atomicbits.scraml.dsl.scalaplay.json.TypedJson._

import play.api.libs.json.{ Format, Json }

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
