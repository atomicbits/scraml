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

import io.atomicbits.scraml.generator.platform.Platform

import Platform._

/**
  * Created by peter on 10/01/17.
  *
  * Transfer Object class definition.
  * In a transfer object class definition, we collect all information that is needed to generate a single TO class,
  * independent from the target language.
  */
case class TransferObjectClassDefinition(reference: ClassReference,
                                         fields: List[Field],
                                         parents: List[ClassReference]          = List.empty,
                                         typeDiscriminator: Option[String]      = None,
                                         typeDiscriminatorValue: Option[String] = None)
    extends SourceDefinition {

  override def classReference(implicit platform: Platform): ClassReference = reference

  def implementingInterfaceReference(implicit platform: Platform): ClassReference = reference.implementingInterfaceReference

  def hasParents: Boolean = parents.nonEmpty

  val actualTypeDiscriminatorValue = typeDiscriminatorValue.getOrElse(reference.canonicalName.name)

}
