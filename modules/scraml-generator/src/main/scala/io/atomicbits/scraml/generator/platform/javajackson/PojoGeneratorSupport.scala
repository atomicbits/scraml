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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.{ ClassReference, JsonTypeInfo, TransferObjectClassDefinition }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 8/03/17.
  */
trait PojoGeneratorSupport {

  implicit def platform: Platform

  def hasOwnInterface(canonicalName: CanonicalName, generationAggr: GenerationAggr): Boolean = {
    generationAggr.isParentInMultipleInheritanceRelation(canonicalName)
  }

  def compileChildrenToSerialize(canonicalName: CanonicalName,
                                 toClassDefinition: TransferObjectClassDefinition,
                                 generationAggr: GenerationAggr): Set[ChildToSerialize] = {

    val isTopLevelParent = !generationAggr.hasParents(canonicalName)

    if (isTopLevelParent) {

      val selfAndChildrenWithClassDefinitions =
        generationAggr
          .allChildren(canonicalName)
          .map { child =>
            val toClassDefinition =
              generationAggr.toMap.getOrElse(child, sys.error(s"Expected to find $child in the generation aggregate."))
            child -> toClassDefinition
          }
          .toMap + (canonicalName -> toClassDefinition)

      selfAndChildrenWithClassDefinitions.map {
        case (child, classDefinition) =>
          val actualClassReference =
            if (hasOwnInterface(child, generationAggr)) classDefinition.implementingInterfaceReference
            else classDefinition.reference
          ChildToSerialize(classReference = actualClassReference, discriminatorValue = classDefinition.actualTypeDiscriminatorValue)
      }.toSet
    } else {
      Set.empty[ChildToSerialize]
    }
  }

  def generateJsonTypeAnnotations(childrenToSerialize: Set[ChildToSerialize], jsonTypeInfo: Option[JsonTypeInfo]): String = {
    if (childrenToSerialize.nonEmpty) {

      val jsonSubTypes =
        childrenToSerialize.map { toSerialize =>
          val discriminatorValue = toSerialize.discriminatorValue
          val name               = toSerialize.classReference.name
          s"""
               @JsonSubTypes.Type(value = $name.class, name = "$discriminatorValue")
             """
        }

      val typeDiscriminator = jsonTypeInfo.map(_.discriminator).getOrElse("type")

      s"""
           @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$typeDiscriminator")
           @JsonSubTypes({
                 ${jsonSubTypes.mkString(",\n")}
           })
         """
    } else {
      ""
    }
  }

  case class ChildToSerialize(classReference: ClassReference, discriminatorValue: String)

}
