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

import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 22/08/15. 
 */
case class RichAction(actionType: ActionType,
                      headers: Map[String, Parameter],
                      queryParameters: Map[String, Parameter],
                      body: Map[String, MimeType],
                      responses: Map[String, Response])

object RichAction {

  def apply(action: Action): RichAction = {

    RichAction(
      actionType = action.actionType,
      headers = action.headers,
      queryParameters = action.queryParameters,
      body = action.body,
      responses = action.responses
    )

  }

}
