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

import io.atomicbits.scraml.ramlparser.model.{ Raml, Resource }

/**
  * Created by peter on 14/01/17.
  */
object ResourceClassSourceDefinitionGenerator {

  // ToDo: work bottom up, start with the source code generation from the code definitions and do the resource classes first

  def resourceToClientAndResourceClassDefinitions(raml: Raml, generationAggr: GenerationAggr): GenerationAggr = {

    def createClientClassDefinition(genAggr: GenerationAggr): GenerationAggr = {
      ???
    }

    def createResourceClassDefinitions(genAggr: GenerationAggr): GenerationAggr = {
      val topLevelResources: List[Resource] = raml.resources

      ???
    }

    createResourceClassDefinitions(createClientClassDefinition(generationAggr))
  }

}
