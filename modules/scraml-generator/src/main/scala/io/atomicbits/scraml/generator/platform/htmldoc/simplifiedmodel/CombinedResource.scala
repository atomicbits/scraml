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
import io.atomicbits.scraml.ramlparser.model.Action

/**
  * Created by peter on 4/06/18.
  */
case class CombinedResource(uniqueId: String,
                            url: String,
                            displayName: Option[String],
                            description: Option[String],
                            actions: List[Action],
                            subResources: List[CombinedResource])

object CombinedResource {

  /**
    * Recursively create the combined resource tree from a given resource class definition.
    */
  def apply(resourceClassDefinition: ResourceClassDefinition): List[CombinedResource] = {

    resourceClassDefinition.resource.actions match {
      case am :: ams =>
        val urlSegments = resourceClassDefinition.precedingUrlSegments :+ resourceClassDefinition.resource.urlSegment
        val combinedResource =
          CombinedResource(
            uniqueId     = UUID.randomUUID().toString,
            url          = urlSegments.mkString("/", "/", ""),
            displayName  = resourceClassDefinition.resource.displayName,
            description  = resourceClassDefinition.resource.description,
            actions      = resourceClassDefinition.resource.actions,
            subResources = resourceClassDefinition.childResourceDefinitions.flatMap(rd => CombinedResource(rd))
          )
        List(combinedResource)
      case Nil => resourceClassDefinition.childResourceDefinitions.flatMap(rd => CombinedResource(rd))
    }
  }

}
