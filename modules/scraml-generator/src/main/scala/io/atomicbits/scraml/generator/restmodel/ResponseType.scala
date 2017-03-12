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

  def apply(response: Response, generationAggr: GenerationAggr)(implicit platform: Platform): Set[ResponseType] = {
    response.body.contentMap.map {
      case (mediaType, bodyContent) =>
        val classPointerOpt = bodyContent.bodyType.flatMap(_.canonical).map(Platform.typeReferenceToClassPointer(_))
        val formParams      = bodyContent.formParameters
        ResponseType(acceptHeader = mediaType, classPointer = classPointerOpt, generationAggr)
    } toSet
  }

  def apply(acceptHeader: MediaType, classPointer: Option[ClassPointer], generationAggr: GenerationAggr)(
      implicit platform: Platform): ResponseType = {

    val mediaTypeValue = acceptHeader.value.toLowerCase

    if (classPointer.isDefined) {
      val classReference          = classPointer.get.native
      val interfaceClassReference = None // generationAggr.getInterfaceDefinition(classReference.canonicalName).map(_.classReference)
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
