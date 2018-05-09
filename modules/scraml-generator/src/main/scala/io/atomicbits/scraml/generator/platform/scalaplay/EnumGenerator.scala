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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.EnumDefinition
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 14/01/17.
  */
case class EnumGenerator(scalaPlay: ScalaPlay) extends SourceGenerator {

  implicit val platform: ScalaPlay = scalaPlay

  def generate(generationAggr: GenerationAggr, enumDefinition: EnumDefinition): GenerationAggr = {

    val imports: Set[String] = platform.importStatements(enumDefinition.reference)

    val source =
      s"""
        package ${enumDefinition.reference.packageName}

        import play.api.libs.json.{Format, Json, JsResult, JsValue, JsString}

        ${imports.mkString("\n")}

        sealed trait ${enumDefinition.reference.name} {
          def name:String
        }

        ${generateEnumCompanionObject(enumDefinition)}
     """

    val sourceFile =
      SourceFile(
        filePath = enumDefinition.reference.toFilePath,
        content  = source
      )

    generationAggr.addSourceFile(sourceFile)
  }

  private def generateEnumCompanionObject(enumDefinition: EnumDefinition): String = {

    // Accompany the enum names with their 'Scala-safe' name.
    val enumsValuesWithSafeName =
      enumDefinition.values map { value =>
        val safeName = scalaPlay.escapeScalaKeyword(CleanNameUtil.cleanEnumName(value))
        value -> safeName
      }

    def enumValue(valueWithSafeName: (String, String)): String = {
      val (value, safeName) = valueWithSafeName
      s"""
         case object $safeName extends ${enumDefinition.reference.name} {
           val name = "$value"
         }
      """
    }

    val enumMapValues =
      enumsValuesWithSafeName
        .map {
          case (value, safeName) => s"$safeName.name -> $safeName"
        }
        .mkString(",")

    val name = enumDefinition.reference.name
    s"""
        object $name {

          ${enumsValuesWithSafeName.map(enumValue).mkString("\n")}

          val byName = Map($enumMapValues)

          implicit val ${name}Format = new Format[$name] {

            override def reads(json: JsValue): JsResult[$name] = {
              json.validate[String].map($name.byName(_))
            }

            override def writes(o: $name): JsValue = {
              JsString(o.name)
            }
          }
        }
       """
  }

}
