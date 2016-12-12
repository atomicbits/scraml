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

package io.atomicbits.scraml.generator.model

import io.atomicbits.scraml.generator.TypeClassRepAssembler
import io.atomicbits.scraml.generator.TypeClassRepAssembler.CanonicalMap
import io.atomicbits.scraml.ramlparser.lookup.TypeLookupTable
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedTypeReference$

import scala.language.postfixOps

/**
  * Created by peter on 22/08/15.
  */
case class RichAction(actionType: Method,
                      headers: ParsedParameters,
                      queryParameters: ParsedParameters,
                      contentTypes: Set[ContentType],
                      responseTypes: Set[ResponseType],
                      selectedContentType: ContentType = NoContentType,
                      selectedResponsetype: ResponseType = NoResponseType)

object RichAction {

  def apply(action: Action,
            lookupTable: TypeLookupTable,
            canonicalMap: CanonicalMap,
            nativeToRootId: NativeId => RootId)(implicit lang: Language): RichAction = {

    def mimeTypeToTypedClassReference(bodyContent: BodyContent): Option[TypedClassReference] = {
      bodyContent.bodyType.collect {
        case theBodyType =>
          new TypeClassRepAssembler(nativeToRootId)
            .typeAsClassReference(theBodyType.parsed, lookupTable, canonicalMap)
            .asTypedClassReference
      }
    }

    val contentTypes = action.body.contentMap.map {
      case (mediaType, bodyContent) => ContentType(mediaType, mimeTypeToTypedClassReference(bodyContent), bodyContent.formParameters)
    } toSet

    // Select the responses in the 200-range and choose the first one present as the main response type that will be accessible as a
    // type in the Response[T] object. Other types will (in the future) be made available as well but require more complex code generation.
    // 200 code range: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
    val range200 = List("200", "201", "202", "203", "204", "205", "206", "207", "208", "226")
    val responseCodesPresent = range200.flatMap(action.responses.get)
    val first200CodePresent = responseCodesPresent.headOption

    val responseTypes =
      first200CodePresent map { response =>
        response.body.contentMap.values.toSet[BodyContent] map { bodyContent =>
          ResponseType(
            acceptHeader = bodyContent.mediaType,
            classReference = mimeTypeToTypedClassReference(bodyContent)
          )
        }
      } getOrElse Set.empty[ResponseType]

    RichAction(
      actionType = action.actionType,
      headers = action.headers,
      queryParameters = action.queryParameters,
      contentTypes = contentTypes,
      responseTypes = responseTypes
    )

  }

}
