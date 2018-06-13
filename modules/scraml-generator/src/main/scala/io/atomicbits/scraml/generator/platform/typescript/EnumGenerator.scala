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
