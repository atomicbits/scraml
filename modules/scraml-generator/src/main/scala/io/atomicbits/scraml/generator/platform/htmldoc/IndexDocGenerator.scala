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

package io.atomicbits.scraml.generator.platform.htmldoc

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.SourceGenerator
import io.atomicbits.scraml.generator.typemodel.ClientClassDefinition
import io.atomicbits.scraml.ramlparser.parser.SourceFile
import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringWriter

import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.platform.htmldoc.simplifiedmodel.ClientDocModel

import scala.collection.JavaConverters._

/**
  * Created by peter on 9/05/18.
  */
object IndexDocGenerator extends SourceGenerator {

  implicit val platform: HtmlDoc.type = HtmlDoc

  def generate(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition): GenerationAggr = {

    val writer   = new StringWriter()
    val mf       = new DefaultMustacheFactory()
    val mustache = mf.compile("platform/htmldoc/index.mustache")

    val clientDocModel = ClientDocModel(clientClassDefinition)
    mustache.execute(writer, toJavaMap(clientDocModel))

    writer.flush()
    val content = writer.toString

    generationAggr.addSourceFile(SourceFile(clientClassDefinition.classReference.toFilePath, content))
  }

  /**
    * Transforms a (case) class content to a Java map recursively.
    *
    * See: https://stackoverflow.com/questions/1226555/case-class-to-map-in-scala
    *
    */
  def toJavaMap(cc: Product, visited: Set[Any] = Set.empty): java.util.Map[String, Any] = {

    def mapValue(f: Any): Any = {
      f match {
        case None                                                     => null
        case Nil                                                      => null
        case l: List[_]                                               => l.map(mapValue).asJava
        case m: Map[_, _]                                             => m.mapValues(mapValue).asJava
        case p: Product if p.productArity > 0 && !visited.contains(p) => toJavaMap(p, visited + cc)
        case x                                                        => x
      }
    }

    // println(s"Processing class ${cc.getClass.getSimpleName}")

    cc.getClass.getDeclaredFields
      .foldLeft(Map.empty[String, Any]) { (fieldMap, field) =>
        field.setAccessible(true)
        val name  = field.getName
        val value = mapValue(field.get(cc))
      fieldMap + (name -> value)
    } asJava

  }

}
