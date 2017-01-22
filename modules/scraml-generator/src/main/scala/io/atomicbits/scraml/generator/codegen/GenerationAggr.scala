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

import io.atomicbits.scraml.generator.typemodel.{ SourceDefinition, SourceFile, TransferObjectInterfaceDefinition }
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
  * @param canonicalToMap The map containing the canonical types of the transfer objects, keyed by their canonical name.
  *                       This map is part of the original input for the generator and will not change any more.
  * @param toInterfaceMap The map containing the transfer objects that require an interface definition so far, keyed on the
  *                       canonical name of the original transfer object. This map is expected to grow while source
  *                       definitions for transfer objects are generated.
  * @param sourceDefinitionsToProcess The collected source definitions up to 'now'.
  */
case class GenerationAggr( // raml: Raml,
                          canonicalToMap: Map[CanonicalName, NonPrimitiveType],
                          toInterfaceMap: Map[CanonicalName, TransferObjectInterfaceDefinition] = Map.empty,
                          sourceDefinitionsToProcess: Seq[SourceDefinition]                     = Seq.empty,
                          sourceFilesGenerated: Seq[SourceFile]                                 = Seq.empty) {

  def addSourceDefinition(sourceDefinition: SourceDefinition): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinition +: sourceDefinitionsToProcess)

  def addSourceDefinitions(sourceDefinitionsToAdd: Seq[SourceDefinition]): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinitionsToProcess ++ sourceDefinitionsToAdd)

  def addSourceFile(sourceFile: SourceFile): GenerationAggr =
    copy(sourceFilesGenerated = sourceFile +: sourceFilesGenerated)

  def addSourceFiles(sourceFiles: Seq[SourceFile]): GenerationAggr =
    copy(sourceFilesGenerated = sourceFiles ++ sourceFilesGenerated)

}
