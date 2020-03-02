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

package io.atomicbits.scraml.generator.restmodel

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.ClassPointer
import io.atomicbits.scraml.ramlparser.model.{ MediaType, Response }

import Platform._

/**
  * Created by peter on 26/08/15.
  */
sealed trait ResponseType {

  def acceptHeader: MediaType

  def acceptHeaderOpt: Option[MediaType] = Some(acceptHeader)

}

case class StringResponseType(acceptHeader: MediaType) extends ResponseType

case class JsonResponseType(acceptHeader: MediaType) extends ResponseType

case class TypedResponseType(acceptHeader: MediaType, classPointer: ClassPointer) extends ResponseType

case class BinaryResponseType(acceptHeader: MediaType) extends ResponseType

case object NoResponseType extends ResponseType {

  val acceptHeader = MediaType("")

  override val acceptHeaderOpt = None

}

object ResponseType {

  def apply(response: Response)(implicit platform: Platform): Set[ResponseType] = {
    response.body.contentMap.map {
      case (mediaType, bodyContent) =>
        val classPointerOpt = bodyContent.bodyType.flatMap(_.canonical).map(Platform.typeReferenceToClassPointer)
        val formParams      = bodyContent.formParameters
        ResponseType(acceptHeader = mediaType, classPointer = classPointerOpt)
    }.toSet
  }

  def apply(acceptHeader: MediaType, classPointer: Option[ClassPointer])(implicit platform: Platform): ResponseType = {

    val mediaTypeValue = acceptHeader.value.toLowerCase

    if (classPointer.isDefined) {
      TypedResponseType(acceptHeader, classPointer.get)
    } else if (mediaTypeValue.contains("json")) {
      JsonResponseType(acceptHeader)
    } else if (mediaTypeValue.contains("text")) {
      StringResponseType(acceptHeader)
    } else if (mediaTypeValue.contains("octet-stream")) {
      BinaryResponseType(acceptHeader)
    } else {
      BinaryResponseType(acceptHeader)
    }

  }

}
