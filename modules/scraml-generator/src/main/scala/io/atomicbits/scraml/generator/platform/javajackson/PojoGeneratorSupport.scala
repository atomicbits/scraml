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
    val hasChildren      = generationAggr.hasChildren(canonicalName)

    if (isTopLevelParent && hasChildren) {

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
