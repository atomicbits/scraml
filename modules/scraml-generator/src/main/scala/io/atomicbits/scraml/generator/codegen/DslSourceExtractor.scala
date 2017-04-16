/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.parser.{ SourceFile, SourceReader }

import scala.util.Try

/**
  * Created by peter on 12/04/17.
  */
object DslSourceExtractor {

  def extract(packageBasePath: List[String])(implicit platform: Platform): Seq[SourceFile[String]] = {

    val bindataSource = Try(SourceReader.read(source = "/io/atomicbits/scraml/jdsl/BinaryData.java"))

    // clazz = classOf[BinaryData]
    // URI is: jar:file:/Users/peter/.ivy2/local/io.atomicbits/scraml-dsl-java/0.7.0-SNAPSHOT/jars/scraml-dsl-java.jar!/io/atomicbits/scraml/jdsl/BinaryData.java
    // URI is: jar:file:/Users/peter/.ivy2/local/io.atomicbits/scraml-dsl-java/0.7.0-SNAPSHOT/jars/scraml-dsl-java.jar!/io/atomicbits/scraml/jdsl/BinaryData.java

    bindataSource.map(println).recover {
      case exc => println("Did not find resource!")
    }

    val bindataSources = Try(SourceReader.readResources("/io/atomicbits/scraml/jdsl", ".java"))

    bindataSources.map { sources =>
      for {
        sourceFile <- sources
      } yield println(sourceFile.filePath)
      println(sources.tail.head.content)
    } recover {
      case exc => println("Did not find resource folder!")
    }

    Seq.empty
  }

}
