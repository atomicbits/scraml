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

    // register all parents for interface generation
    val parentInterfacesToGenerate =
      recursiveExtendedParents.map(TransferObjectInterfaceDefinition(_, discriminator))

    val generationAggrWithParentInterfaces =
      parentInterfacesToGenerate.foldLeft(generationAggr) { (aggr, parentInt) =>
        aggr.addInterfaceSourceDefinition(parentInt)
      }

    val interfaceDefinition = TransferObjectInterfaceDefinition(toClassDefinition, discriminator)

    generationAggrWithParentInterfaces.addInterfaceSourceDefinition(interfaceDefinition) // We only generate interfaces
  }

}
