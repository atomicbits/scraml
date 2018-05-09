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

package io.atomicbits.scraml.ramlparser.parser

import java.nio.file.{ Path, Paths }

import io.atomicbits.scraml.ramlparser.model.{ JsInclude, Raml }
import play.api.libs.json._

import scala.util.Try

/**
  * Created by peter on 6/02/16.
  */
case class RamlParser(ramlSource: String, charsetName: String) {

  def parse: Try[Raml] = {
    val JsonFile(path, ramlJson) = RamlToJsonParser.parseToJson(ramlSource, charsetName)
    val parsed: JsObject =
      ramlJson match {
        case ramlJsObj: JsObject => parseRamlJsonDocument(path.getParent, ramlJsObj)
        case x                   => sys.error(s"Could not parse $ramlSource, expected a RAML document.")
      }

    val parseContext = ParseContext(List(ramlSource), List.empty)

    Raml(parsed)(parseContext)
  }

  /**
    * Recursively parse all RAML documents by following all include statements and packing everything in one big JSON object.
    * The source references will be injected under the "_source" fields so that we can trace the origin of all documents later on.
    *
    * @param raml
    */
  private def parseRamlJsonDocument(basePath: Path, raml: JsObject): JsObject = {

    def parseNested(doc: JsValue, currentBasePath: Path): JsValue = {
      doc match {
        case JsInclude(source) =>
          // The check for empty base path below needs to be there for Windows machines, to avoid paths like "/C:/Users/..."
          // that don't resolve because of the leading "/".
          // ToDo: Refactor to use file system libraries to merge paths (and test on Windows as well).
          val nextPath =
            if (currentBasePath.normalize().toString.isEmpty) Paths.get(source)
            else currentBasePath.resolve(source) // s"$currentBasePath/$source"
          val JsonFile(newFilePath, included) = RamlToJsonParser.parseToJson(nextPath.normalize().toString)
          included match {
            case incl: JsObject => parseNested(incl + (Sourced.sourcefield -> JsString(source)), newFilePath.getParent)
            case x              => parseNested(x, newFilePath.getParent)
          }
        case jsObj: JsObject =>
          val mappedFields = jsObj.fields.collect {
            case (key, value) => key -> parseNested(value, currentBasePath)
          }
          JsObject(mappedFields)
        case jsArr: JsArray => JsArray(jsArr.value.map(parseNested(_, currentBasePath)))
        case x              => x
      }
    }

    parseNested(raml, basePath).asInstanceOf[JsObject]
  }

}
