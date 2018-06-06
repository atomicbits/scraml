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

import java.util.UUID

import io.atomicbits.scraml.generator.typemodel.ResourceClassDefinition
import io.atomicbits.scraml.ramlparser.model.{ Action, Parameter }

/**
  * Created by peter on 4/06/18.
  */
case class CombinedResource(uniqueId: String,
                            url: String,
                            urlParameters: List[Parameter],
                            displayName: Option[String],
                            description: Option[String],
                            actions: List[SimpleAction],
                            subResources: List[CombinedResource])

object CombinedResource {

  /**
    * Recursively create the combined resource tree from a given resource class definition.
    */
  def apply(resourceClassDefinition: ResourceClassDefinition,
            parentUrl: String              = "",
            urlParameters: List[Parameter] = List.empty): List[CombinedResource] = {

    val combinedUrlPrameters = resourceClassDefinition.resource.urlParameter.toList ++ urlParameters

    val url = resourceClassDefinition.resource.urlParameter match {
      case None        => s"$parentUrl/${resourceClassDefinition.resource.urlSegment}"
      case Some(param) => s"$parentUrl/{${resourceClassDefinition.resource.urlSegment}}"
    }

    resourceClassDefinition.resource.actions match {
      case am :: ams =>
        val combinedResource =
          CombinedResource(
            uniqueId      = UUID.randomUUID().toString,
            url           = url,
            urlParameters = combinedUrlPrameters,
            displayName   = resourceClassDefinition.resource.displayName,
            description   = resourceClassDefinition.resource.description,
            actions       = resourceClassDefinition.resource.actions.map(SimpleAction(_)),
            subResources  = resourceClassDefinition.childResourceDefinitions.flatMap(rd => CombinedResource(rd, url, combinedUrlPrameters))
          )
        List(combinedResource)
      case Nil =>
        resourceClassDefinition.childResourceDefinitions.flatMap(rd => CombinedResource(rd, url, combinedUrlPrameters))
    }
  }

}
