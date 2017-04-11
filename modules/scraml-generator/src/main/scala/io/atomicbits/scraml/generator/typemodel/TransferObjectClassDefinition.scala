/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
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
