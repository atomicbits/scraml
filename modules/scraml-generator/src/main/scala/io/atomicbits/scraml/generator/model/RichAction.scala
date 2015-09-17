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

import io.atomicbits.scraml.generator.lookup.SchemaLookup
import io.atomicbits.scraml.parser.model._

import scala.language.postfixOps

/**
 * Created by peter on 22/08/15. 
 */
case class RichAction(actionType: ActionType,
                      queryParameters: Map[String, Parameter],
                      contentTypes: Set[ContentType],
                      responseTypes: Set[ResponseType],
                      headers: Map[String, Parameter],
                      selectedContentType: ContentType = NoContentType,
                      selectedResponsetype: ResponseType = NoResponseType)

object RichAction {

  def apply(action: Action, schemaLookup: SchemaLookup): RichAction = {

    def mimeTypeToClassRep(mimeType: MimeType): Option[TypedClassReference] = {
      mimeType.schema.flatMap(schemaLookup.externalSchemaLinks.get).map(schemaLookup.rootIdAsTypedClassReference)
    }

    val contentTypes = action.body.values.toList map { mimeType =>
      ContentType(
        contentTypeHeader = mimeType.mimeType,
        classReference = mimeTypeToClassRep(mimeType),
        formParameters = mimeType.formParameters
      )
    } toSet

    val responseTypes =
      action.responses.get("200") map { response =>
        response.body.values.toSet[MimeType] map { mimeType =>
          ResponseType(
            acceptHeader = mimeType.mimeType,
            classReference = mimeTypeToClassRep(mimeType)
          )
        }
      }  getOrElse Set.empty[ResponseType]

    RichAction(
      actionType = action.actionType,
      queryParameters = action.queryParameters,
      contentTypes = contentTypes,
      responseTypes = responseTypes,
      headers = action.headers
    )

  }

}
