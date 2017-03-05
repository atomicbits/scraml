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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ HeaderSegmentClassDefinition, SourceFile }
import io.atomicbits.scraml.generator.platform.Platform._

/**
  * Created by peter on 1/03/17.
  */
object HeaderSegmentClassGenerator extends SourceGenerator {

  import Platform._

  implicit val platform: Platform = JavaJackson

  def generate(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition): GenerationAggr = {

    val className   = headerSegmentClassDefinition.reference.name
    val packageName = headerSegmentClassDefinition.reference.packageName
    val imports     = platform.importStatements(headerSegmentClassDefinition.reference, headerSegmentClassDefinition.imports)
    val methods     = headerSegmentClassDefinition.methods

    val source =
      s"""
         package $packageName;

         import io.atomicbits.scraml.jdsl.*;
         import java.util.*;
         import java.util.concurrent.CompletableFuture;
         import java.io.*;

         ${imports.mkString(";\n")};


         public class $className extends HeaderSegment {

           public $className(RequestBuilder requestBuilder) {
             super(requestBuilder);
           }

           ${methods.mkString("\n")}

         }
       """

    val filePath = platform.classReferenceToFilePath(headerSegmentClassDefinition.reference)

    generationAggr.addSourceFile(SourceFile(filePath = filePath, content = source))
  }

}
