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
import io.atomicbits.scraml.ramlparser.model._

/**
  * Created by peter on 6/06/18.
  */
case class SimpleAction(actionType: Method,
                        headers: List[SimpleParameter],
                        queryParameters: List[SimpleParameter],
                        body: SimpleBody,
                        responses: List[SimpleResponse],
                        queryString: Option[QueryString] = None,
                        description: Option[String]      = None)

object SimpleAction {

  def apply(action: Action, generationAggr: GenerationAggr): SimpleAction = {
    SimpleAction(
      actionType      = action.actionType,
      headers         = action.headers.values.map(SimpleParameter(_, generationAggr)),
      queryParameters = action.queryParameters.values.map(SimpleParameter(_, generationAggr)),
      body            = SimpleBody(action.body, generationAggr),
      responses       = action.responses.values.map(res => SimpleResponse(res, generationAggr)),
      queryString     = action.queryString,
      description     = action.description
    )
  }

}
