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

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.EnumDefinition
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 1/03/17.
  */
case class EnumGenerator(javaJackson: CommonJavaJacksonPlatform) extends SourceGenerator {

  implicit val platform: Platform = javaJackson

  def generate(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr = {

    // Accompany the enum names with their 'Java-safe' name.
    val enumsWithSafeName =
      enumDefinition.values map { value =>
        val safeName = javaJackson.escapeJavaKeyword(CleanNameUtil.cleanEnumName(value))
        s"""$safeName("$value")""" // e.g. space("spa ce")
      }

    val classNameCamel = CleanNameUtil.camelCased(enumDefinition.reference.name)

    val source =
      s"""
        package ${enumDefinition.reference.packageName};

        import com.fasterxml.jackson.annotation.*;

        public enum ${enumDefinition.reference.name} {

          ${enumsWithSafeName.mkString("", ",\n", ";\n")}

          private final String value;

          private ${enumDefinition.reference.name}(final String value) {
                  this.value = value;
          }

          @JsonValue
          final String value() {
            return this.value;
          }

          @JsonCreator
          public static ${enumDefinition.reference.name} fromValue(String value) {
            for (${enumDefinition.reference.name} $classNameCamel : ${enumDefinition.reference.name}.values()) {
              if (value.equals($classNameCamel.value())) {
                return $classNameCamel;
              }
            }
            throw new IllegalArgumentException("Cannot instantiate a ${enumDefinition.reference.name} enum element from " + value);
          }

          public String toString() {
            return value();
          }

        }
       """

    val sourceFile =
      SourceFile(
        filePath = enumDefinition.reference.toFilePath,
        content  = source
      )

    generationAggr.addSourceFile(sourceFile)
  }

}
