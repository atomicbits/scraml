/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.platform.typescript

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.SourceGenerator
import io.atomicbits.scraml.generator.typemodel.{ TransferObjectClassDefinition, TransferObjectInterfaceDefinition }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 15/12/17.
  */
case class ToClassGenerator(typeScript: TypeScript) extends SourceGenerator {

  val defaultDiscriminator = "type"

  implicit val platform: TypeScript = typeScript

  def generate(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr = {

    val originalToCanonicalName = toClassDefinition.reference.canonicalName

    val parentNames: List[CanonicalName] = generationAggr.allParents(originalToCanonicalName)

    val recursiveExtendedParents =
      parentNames.foldLeft(Seq.empty[TransferObjectClassDefinition]) { (aggr, parentName) =>
        val interfaces = aggr
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        interfaces :+ parentDefinition
      }

    val discriminator: String =
      (toClassDefinition.typeDiscriminator +: recursiveExtendedParents.map(_.typeDiscriminator)).flatten.headOption
        .getOrElse(defaultDiscriminator)

    val interfaceDefinition = TransferObjectInterfaceDefinition(toClassDefinition, discriminator)

    generationAggr.addInterfaceSourceDefinition(interfaceDefinition) // We only generate interfaces
  }

}
