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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.{ DslSourceRewriter, GenerationAggr }
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.HeaderSegmentClassDefinition
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 1/03/17.
  */
case class HeaderSegmentClassGenerator(javaJackson: CommonJavaJacksonPlatform) extends SourceGenerator {

  import Platform._

  implicit val platform: Platform = javaJackson

  def generate(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition): GenerationAggr = {

    val className   = headerSegmentClassDefinition.reference.name
    val packageName = headerSegmentClassDefinition.reference.packageName
    val imports     = platform.importStatements(headerSegmentClassDefinition.reference, headerSegmentClassDefinition.imports)
    val methods     = headerSegmentClassDefinition.methods

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val source =
      s"""
         package $packageName;

         import $dslBasePackage.*;
         import java.util.*;
         import java.util.concurrent.CompletableFuture;
         import java.io.*;

         ${imports.mkString("\n")};


         public class $className extends HeaderSegment {

           public $className(RequestBuilder requestBuilder) {
             super(requestBuilder);
           }

           ${methods.mkString("\n")}

         }
       """

    val filePath = headerSegmentClassDefinition.reference.toFilePath

    generationAggr.addSourceFile(SourceFile(filePath = filePath, content = source))
  }

}
