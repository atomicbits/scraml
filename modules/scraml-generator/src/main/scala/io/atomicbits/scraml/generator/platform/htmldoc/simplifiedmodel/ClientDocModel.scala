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

import io.atomicbits.scraml.generator.typemodel.ClientClassDefinition

/**
  * Created by peter on 4/06/18.
  */
case class ClientDocModel(apiName: String,
                          baseUri: Option[String],
                          combinedResources: List[CombinedResource],
                          title: String,
                          version: String,
                          description: String)

object ClientDocModel {

  def apply(clientClassDefinition: ClientClassDefinition): ClientDocModel = {

    ClientDocModel(
      apiName           = clientClassDefinition.apiName,
      baseUri           = clientClassDefinition.baseUri,
      combinedResources = clientClassDefinition.topLevelResourceDefinitions.flatMap(rd => CombinedResource(rd)),
      title             = clientClassDefinition.title,
      version           = clientClassDefinition.version,
      description       = clientClassDefinition.description
    )
  }

}
