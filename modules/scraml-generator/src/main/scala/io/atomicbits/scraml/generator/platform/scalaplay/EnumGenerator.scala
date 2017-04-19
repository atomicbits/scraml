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

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

  private def generateEnumCompanionObject(enumDefinition: EnumDefinition): String = {

    // Accompany the enum names with their 'Scala-safe' name.
    val enumsValuesWithSafeName =
      enumDefinition.values map { value =>
        val safeName = CleanNameUtil.escapeScalaKeyword(CleanNameUtil.cleanEnumName(value))
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
