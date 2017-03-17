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

  lazy val urlParamClassPointer: Option[ClassPointer] = {
    resource.urlParameter
      .map(_.parameterType)
      .flatMap(_.canonical)
      .map(Platform.typeReferenceToClassPointer(_))
  }

  override def classReference(implicit platform: Platform): ClassReference = {

    val resourceClassName = s"${CleanNameTools.cleanClassName(resource.urlSegment)}Resource"

    ClassReference(name = resourceClassName, packageParts = resourcePackage)
  }

}
