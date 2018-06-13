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

package io.atomicbits.scraml.generator.typemodel

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.ramlparser.model.Resource

/**
  * Created by peter on 13/01/17.
  *
  * In a resource class definition, we collect all information that is needed to generate a single resource class, independent from
  * the target language.
  */
case class ResourceClassDefinition(apiPackage: List[String], precedingUrlSegments: List[String], resource: Resource)
    extends SourceDefinition {

  val nextPackagePart: String = CleanNameTools.cleanPackageName(resource.urlSegment)

  lazy val childResourceDefinitions: List[ResourceClassDefinition] = {
    val nextPrecedingUrlSegments = precedingUrlSegments :+ nextPackagePart
    resource.resources.map { childResource =>
      ResourceClassDefinition(
        apiPackage           = apiPackage,
        precedingUrlSegments = nextPrecedingUrlSegments,
        resource             = childResource
      )
    }
  }

  lazy val resourcePackage: List[String] = apiPackage ++ precedingUrlSegments :+ nextPackagePart

  def urlParamClassPointer(): Option[ClassPointer] = {
    resource.urlParameter
      .map(_.parameterType)
      .flatMap(_.canonical)
      .map(Platform.typeReferenceToClassPointer)
  }

  override def classReference(implicit platform: Platform): ClassReference = {

    val resourceClassName = s"${CleanNameTools.cleanClassName(resource.urlSegment)}Resource"

    ClassReference(name = resourceClassName, packageParts = resourcePackage)
  }

}
