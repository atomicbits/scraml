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

package io.atomicbits.scraml.generator.platform.htmldoc.simplifiedmodel

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.ramlparser.model.{ BodyContent, MediaType, Parameters, TypeRepresentation }

/**
  * Created by peter on 7/06/18.
  */
case class SimpleBodyContent(mediaType: MediaType, bodyType: Option[TypeRepresentation], formParameters: Parameters, html: String)

object SimpleBodyContent {

  def apply(bodyContent: BodyContent, generationAggr: GenerationAggr): SimpleBodyContent = {
    val html = {
      for {
        bt <- bodyContent.bodyType
        canonical <- bt.canonical
      } yield BodyContentRenderer(generationAggr).renderHtmlForType(canonical)
    } getOrElse ""

    SimpleBodyContent(
      mediaType      = bodyContent.mediaType,
      bodyType       = bodyContent.bodyType,
      formParameters = bodyContent.formParameters,
      html           = html
    )
  }

}
