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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ ClassReference, ResourceClassDefinition, SourceFile }
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.model.Resource
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.TypeReference

/**
  * Created by peter on 14/01/17.
  */
object ResourceClassGenerator extends SourceGenerator {

  implicit val platform: Platform = ScalaPlay

  def generate(resourceClassDefinition: ResourceClassDefinition): List[SourceFile] = {
    ???
  }

  def generateResourceDslField(packageBasePath: List[String], resource: Resource): String = {

    val cleanUrlSegment  = ScalaPlay.escapeScalaKeyword(CleanNameTools.cleanMethodName(resource.urlSegment))
    val resourceClassRef = createResourceClassReference(packageBasePath, resource)
    val canonicalUrlParameterTypeOpt: Option[TypeReference] =
      resource.urlParameter
        .map(_.parameterType)
        .flatMap(_.canonical)

    canonicalUrlParameterTypeOpt match {
      case Some(typeReference) =>
        val urlParamClassReference: ClassReference = Platform.typeReferenceToClassPointer(typeReference).native
        val urlParamClassName                      = urlParamClassReference.name
        s"""def $cleanUrlSegment(value: $urlParamClassName) = new ${resourceClassRef.fullyQualifiedName}(value, _requestBuilder.withAddedPathSegment(value))"""
      case None =>
        s"""def $cleanUrlSegment = new ${resourceClassRef.fullyQualifiedName}(_requestBuilder.withAddedPathSegment("${resource.urlSegment}"))"""
    }
  }

  def createResourceClassReference(packageBasePath: List[String], resource: Resource): ClassReference = {

    val resourceClassName = s"${CleanNameTools.cleanClassName(resource.urlSegment)}Resource"

    val nextPackagePart = CleanNameTools.cleanPackageName(resource.urlSegment)

    val nextPackageBasePath = packageBasePath :+ nextPackagePart

    ClassReference(name = resourceClassName, packageParts = nextPackageBasePath)
  }

}
