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

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.ClassPointer
import io.atomicbits.scraml.ramlparser.model.{ Body, MediaType }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedParameters

/**
  * Created by peter on 26/08/15.
  */
sealed trait ContentType {

  def contentTypeHeader: MediaType

  def contentTypeHeaderOpt: Option[MediaType] = Some(contentTypeHeader)

}

case class StringContentType(contentTypeHeader: MediaType) extends ContentType

case class JsonContentType(contentTypeHeader: MediaType) extends ContentType

case class TypedContentType(contentTypeHeader: MediaType, classPointer: ClassPointer) extends ContentType

case class FormPostContentType(contentTypeHeader: MediaType, formParameters: ParsedParameters) extends ContentType

case class MultipartFormContentType(contentTypeHeader: MediaType) extends ContentType

case class BinaryContentType(contentTypeHeader: MediaType) extends ContentType

case class AnyContentType(contentTypeHeader: MediaType) extends ContentType

case object NoContentType extends ContentType {

  val contentTypeHeader = MediaType("")

  override val contentTypeHeaderOpt = None

}

object ContentType {

  def apply(body: Body): Set[ContentType] =
    body.contentMap.map {
      case (mediaType, bodyContent) =>
        val classPointerOpt = bodyContent.bodyType.flatMap(_.canonical).map(Platform.typeReferenceToClassPointer(_))
        val formParams      = bodyContent.formParameters
        ContentType(mediaType = mediaType, content = classPointerOpt, formParameters = formParams)
    } toSet

  def apply(mediaType: MediaType, content: Option[ClassPointer], formParameters: ParsedParameters): ContentType = {

    val mediaTypeValue = mediaType.value.toLowerCase

    if (mediaTypeValue == "multipart/form-data") {
      MultipartFormContentType(mediaType)
    } else if (formParameters.nonEmpty) {
      FormPostContentType(mediaType, formParameters)
    } else if (content.isDefined) {
      TypedContentType(mediaType, content.get)
    } else if (mediaTypeValue.contains("json")) {
      JsonContentType(mediaType)
    } else if (mediaTypeValue.contains("text")) {
      StringContentType(mediaType)
    } else if (mediaTypeValue.contains("octet-stream")) {
      BinaryContentType(mediaType)
    } else {
      AnyContentType(mediaType)
    }
  }

}
