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
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.EnumDefinition
import io.atomicbits.scraml.ramlparser.parser.SourceFile
import Platform._

/**
  * Created by peter on 15/12/17.
  */
case class EnumGenerator(typeScript: TypeScript) extends SourceGenerator {

  implicit val platform: TypeScript = typeScript

  def generate(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr = {

    val enumName   = enumDefinition.reference.name
    val enumValues = enumDefinition.values.map(CleanNameTools.quoteString)

    val source =
      s"""
         |export type ${enumName} = ${enumValues.mkString(" | ")} 
       """.stripMargin

    val sourceFile =
      SourceFile(
        filePath = enumDefinition.classReference.toFilePath,
        content  = source
      )

    generationAggr.addSourceFile(sourceFile)
  }

}
