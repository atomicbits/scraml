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

package io.atomicbits.scraml.generator.oldmodel

import io.atomicbits.scraml.generator.TypeClassRepAssembler.CanonicalMap
import io.atomicbits.scraml.ramlparser.lookup.OldCanonicalLookupHelper
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedParameter
import io.atomicbits.scraml.ramlparser.model.{ NativeId, Resource, RootId }

/**
  * Created by peter on 22/08/15.
  */
case class RichResource(urlSegment: String,
                        urlParameter: Option[ParsedParameter] = None,
                        classRep: ClassRep,
                        actions: List[RichAction]     = List.empty,
                        resources: List[RichResource] = List.empty)

object RichResource {

  def apply(resource: Resource,
            packageBasePath: List[String],
            typeLookupTable: OldCanonicalLookupHelper,
            canonicalMap: CanonicalMap,
            nativeToRootId: NativeId => RootId)(implicit lang: Language): RichResource = {

    def createRichResource(resource: Resource, actualPackageBasePath: List[String]): RichResource = {

      val resourceClassName = s"${CleanNameUtil.cleanClassName(resource.urlSegment)}Resource"

      val nextPackagePart = CleanNameUtil.cleanPackageName(resource.urlSegment)

      val nextPackageBasePath = actualPackageBasePath :+ nextPackagePart

      val richChildResources = resource.resources.map(createRichResource(_, nextPackageBasePath))

      val richActions = resource.actions.map(RichAction(_, typeLookupTable, canonicalMap, nativeToRootId))

      RichResource(
        urlSegment   = resource.urlSegment,
        urlParameter = resource.urlParameter,
        classRep = ClassRep(
          classReference = ClassReference(
            name         = resourceClassName,
            packageParts = nextPackageBasePath
          )
        ),
        actions   = richActions,
        resources = richChildResources
      )

    }

    createRichResource(resource, packageBasePath)

  }

}
