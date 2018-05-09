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

package io.atomicbits.scraml.ramlparser.model

/**
  * Created by peter on 26/08/16.
  */
trait MediaType {

  def value: String

}
case class ActualMediaType(value: String) extends MediaType

case object NoMediaType extends MediaType {

  val value: String = ""

}

object MediaType {

  def apply(mimeType: String): MediaType = unapply(mimeType).getOrElse(NoMediaType)

  def unapply(mimeType: String): Option[MediaType] = {
    val (typeAndSubT, params) =
      mimeType.split(';').toList match {
        case typeAndSubType :: Nil                      => (Some(typeAndSubType.trim), None)
        case typeAndSubType :: parameters :: unexpected => (Some(typeAndSubType.trim), Some(parameters.trim))
        case Nil                                        => (None, None)
      }

    typeAndSubT.flatMap { typeSub =>
      typeSub.split('/').toList match {
        case ttype :: subtype :: anything => Some(ActualMediaType(typeSub))
        case _                            => None
      }
    }
  }

}
