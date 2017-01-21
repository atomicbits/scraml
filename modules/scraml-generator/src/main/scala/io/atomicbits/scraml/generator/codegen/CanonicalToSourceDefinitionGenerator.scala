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

import io.atomicbits.scraml.generator.typemodel.{ EnumDefinition, TransferObjectClassDefinition, UnionClassDefinition }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._

/**
  * Created by peter on 14/01/17.
  *
  * Transforms a map containing canonical RAML or json-schema types to a sequence of source definitions. The number of
  * source definitions isn't always equal to the number of canonical types, because multiple inheritance may produce additional
  * source definitions.
  */
object CanonicalToSourceDefinitionGenerator {

  def transferObjectsToClassDefinitions(generationAggr: GenerationAggr): GenerationAggr = {

    // ToDo: see if the following assertion is true for all languages, for now we put this logic in the sourcecode generators.
    // We assume here that all our target languages support inheritance, but no multiple inheritance and that
    // all languages have a way to express interface definitions in order to 'simulate' the effect, to some extend,
    // of multiple inheritance.

    // Conclusion so far:
    // * SourceDefinitions are generated independent of the target language
    // * interface definitions are not generated now, its the target language's responsibility to generate them when necessary while
    //   generating the source codes (adding those interface definitions to the GenerationAggr for later source code generation).

    def objectTypeToTransferObjectClassDefinition(objectType: ObjectType): TransferObjectClassDefinition = {
      ???
    }

    def enumTypeToEnumDefinition(enumType: EnumType): EnumDefinition = {
      ???
    }

    def unionTypeToUnionClassDefinition(unionType: UnionType): UnionClassDefinition = {
      ???
    }

    generationAggr.canonicalToMap.values.foldLeft(generationAggr) { (genAggr, theType) =>
      val sourceDefinition =
        theType match {
          case objectType: ObjectType => objectTypeToTransferObjectClassDefinition(objectType)
          case enumType: EnumType     => enumTypeToEnumDefinition(enumType)
          case unionType: UnionType   => unionTypeToUnionClassDefinition(unionType)
          case unexpected             => sys.error(s"Unexpected type seen during TO definition generation: $unexpected")
        }
      genAggr.addSourceDefinition(sourceDefinition)
    }

    ???
  }

}
