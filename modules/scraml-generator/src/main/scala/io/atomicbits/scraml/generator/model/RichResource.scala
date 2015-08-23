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

import io.atomicbits.scraml.generator.CleanNameUtil
import io.atomicbits.scraml.parser.model.{Parameter, Resource}

/**
 * Created by peter on 22/08/15. 
 */
case class RichResource(urlSegment: String,
                        urlParameter: Option[Parameter] = None,
                        packageParts: List[String],
                        resourceClassName: String,
                        actions: List[RichAction] = List.empty,
                        resources: List[RichResource] = List.empty)


object RichResource {

  def apply(resource: Resource, packageBasePath: List[String]): RichResource = {

    def createRichResource(resource: Resource, actualPackagePath: List[String]): RichResource = {

      val resourceClassName = s"${CleanNameUtil.cleanClassName(resource.urlSegment)}Resource"

      val nextPackagePart = CleanNameUtil.cleanPackageName(resource.urlSegment)

      val richChildResources = resource.resources.map(createRichResource(_, actualPackagePath :+ nextPackagePart))

      val richActions = resource.actions.map(RichAction(_))

      RichResource(
        urlSegment = resource.urlSegment,
        urlParameter = resource.urlParameter,
        packageParts = actualPackagePath,
        resourceClassName = resourceClassName,
        actions = richActions,
        resources = richChildResources
      )

    }

    createRichResource(resource, packageBasePath)

  }

}
