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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, NonPrimitiveType }

/**
  * Created by peter on 18/01/17.
  *
  * The generation aggregate contains information that may be needed for source definition generation and information
  * about data and knownledge that was already collected so far in the generation process. The generation process is
  * a recursive operation on the GenerationAggr, which can be expanded during code generation. In other words, new source
  * definitions may be added during code generation, especially the interface definitions are expected to be added then.
  *
  * @param sourceDefinitionsToProcess The collected source definitions up to 'now'.
  * @param sourceFilesGenerated The generated source files so far.
  * @param toInterfaceMap The map containing the transfer objects that require an interface definition so far, keyed on the
  *                       canonical name of the original transfer object. This map is expected to grow while source
  *                       definitions for transfer objects are generated.
  */
case class GenerationAggr(sourceDefinitionsToProcess: Seq[SourceDefinition]                     = Seq.empty,
                          sourceFilesGenerated: Seq[SourceFile]                                 = Seq.empty,
                          toInterfaceMap: Map[CanonicalName, TransferObjectInterfaceDefinition] = Map.empty) {

  def addSourceDefinition(sourceDefinition: SourceDefinition): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinition +: sourceDefinitionsToProcess)

  def addSourceDefinitions(sourceDefinitionsToAdd: Seq[SourceDefinition]): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinitionsToProcess ++ sourceDefinitionsToAdd)

  def addSourceFile(sourceFile: SourceFile): GenerationAggr =
    copy(sourceFilesGenerated = sourceFile +: sourceFilesGenerated)

  def addSourceFiles(sourceFiles: Seq[SourceFile]): GenerationAggr =
    copy(sourceFilesGenerated = sourceFiles ++ sourceFilesGenerated)

  def generate(implicit platform: Platform): GenerationAggr = {

    import Platform._

    sourceDefinitionsToProcess match {
      case srcDef :: srcDefs => srcDef.toSourceFile(this).dropSourceDefinitionsHead.generate
      case Nil               => this
    }

  }

  private def dropSourceDefinitionsHead: GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinitionsToProcess.tail)

}

object GenerationAggr {

  def apply(apiName: String,
            apiBasePackage: List[String],
            raml: Raml,
            canonicalToMap: Map[CanonicalName, NonPrimitiveType]): GenerationAggr = {

    def collectResourceDefinitions(
        resourceDefinitionsToProcess: List[ResourceClassDefinition],
        collectedResourceDefinitions: List[ResourceClassDefinition] = List.empty): List[ResourceClassDefinition] = {

      resourceDefinitionsToProcess match {
        case Nil => collectedResourceDefinitions
        case _ =>
          val childResourceDefinitions = resourceDefinitionsToProcess.flatMap(_.childResourceDefinitions)
          collectResourceDefinitions(childResourceDefinitions, collectedResourceDefinitions ++ resourceDefinitionsToProcess)
      }
    }

    val topLevelResourceDefinitions = raml.resources.map(ResourceClassDefinition(apiBasePackage, List.empty, _))

    val clientClassDefinition =
      ClientClassDefinition(
        apiName                     = apiName,
        baseUri                     = raml.baseUri,
        basePackage                 = apiBasePackage,
        topLevelResourceDefinitions = topLevelResourceDefinitions
      )

    val collectedResourceDefinitions = collectResourceDefinitions(topLevelResourceDefinitions)

    val sourceDefinitions: Seq[SourceDefinition] = clientClassDefinition +: collectedResourceDefinitions

    val generationAggrBeforeCanonicalDefinitions = GenerationAggr(sourceDefinitions)

    val finalGenerationAggregate: GenerationAggr =
      CanonicalToSourceDefinitionGenerator.transferObjectsToClassDefinitions(generationAggrBeforeCanonicalDefinitions, canonicalToMap)

    finalGenerationAggregate
  }

}
