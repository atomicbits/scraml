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

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.ClassPointer
import io.atomicbits.scraml.ramlparser.model.{ Body, MediaType, Parameters }

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

case class FormPostContentType(contentTypeHeader: MediaType, formParameters: Parameters) extends ContentType

case class MultipartFormContentType(contentTypeHeader: MediaType) extends ContentType

case class BinaryContentType(contentTypeHeader: MediaType) extends ContentType

case class AnyContentType(contentTypeHeader: MediaType) extends ContentType

case object NoContentType extends ContentType {

  val contentTypeHeader = MediaType("")

  override val contentTypeHeaderOpt = None

}

object ContentType {

  def apply(body: Body)(implicit platform: Platform): Set[ContentType] =
    body.contentMap.map {
      case (mediaType, bodyContent) =>
        val classPointerOpt = bodyContent.bodyType.flatMap(_.canonical).map(Platform.typeReferenceToClassPointer)
        val formParams      = bodyContent.formParameters
        ContentType(mediaType = mediaType, content = classPointerOpt, formParameters = formParams)
    }.toSet

  def apply(mediaType: MediaType, content: Option[ClassPointer], formParameters: Parameters)(implicit platform: Platform): ContentType = {

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
