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

import io.atomicbits.scraml.generator.ClassRep
import io.atomicbits.scraml.generator.lookup.SchemaLookup
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 22/08/15. 
 */
case class RichAction(actionType: ActionType,
                      queryParameters: Map[String, Parameter],
                      contentTypes: List[ContentType],
                      responseTypes: List[ResponseType],
                      headers: Map[String, Parameter])

object RichAction {

  def apply(action: Action, schemaLookup: SchemaLookup): RichAction = {

    def mimeTypeToClassRep(mimeType: MimeType): Option[ClassRep] = {
      mimeType.schema.flatMap(schemaLookup.externalSchemaLinks.get).flatMap(schemaLookup.classReps.get)
    }

    val contentTypes = action.body.values.toList map { mimeType =>
      ContentType(
        contentTypeHeader = mimeType.mimeType,
        classRep = mimeTypeToClassRep(mimeType),
        formParameters = mimeType.formParameters
      )
    }

    val responseTypes =
      action.responses.values.toList flatMap { response =>
        response.body.values.toList map { mimeType =>
          ResponseType(
            acceptHeader = mimeType.mimeType,
            classRep = mimeTypeToClassRep(mimeType)
          )
        }

      }

    RichAction(
      actionType = action.actionType,
      queryParameters = action.queryParameters,
      contentTypes = contentTypes,
      responseTypes = responseTypes,
      headers = action.headers
    )

  }

}
