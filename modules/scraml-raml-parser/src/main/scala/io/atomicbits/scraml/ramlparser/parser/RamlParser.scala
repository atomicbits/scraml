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

package io.atomicbits.scraml.ramlparser.parser


import io.atomicbits.scraml.ramlparser.model.{Id, JsInclude, Raml, RootId}
import play.api.libs.json._

import scala.util.Try


/**
  * Created by peter on 6/02/16.
  */
case class RamlParser(ramlSource: String, charsetName: String, defaultPackage: List[String]) {


  def parse: Try[Raml] = {
    val (path, ramlJson) = RamlToJsonParser.parseToJson(ramlSource, charsetName)
    val parsed: JsObject =
      ramlJson match {
        case ramlJsObj: JsObject => parseRamlJsonDocument(path, ramlJsObj)
        case x                   => sys.error(s"Could not parse $ramlSource, expected a RAML document.")
      }

    require(
      defaultPackage.length > 1,
      s"The default package should contain at least 2 fragments, now it has only one or less: $defaultPackage."
    )

    val host = defaultPackage.take(2).reverse.mkString(".")
    val urlPath = defaultPackage.drop(2).mkString("/")

    val nameToId: String => Id = name => RootId(s"http://$host/$urlPath/$name.json")
    val parseContext = ParseContext(List(ramlSource), nameToId)

    Raml(parsed)(parseContext)
  }

  /**
    * Recursively parse all RAML documents by following all include statements and packing everything in one big JSON object.
    * The source references will be injected under the "_source" fields so that we can trace the origin of all documents later on.
    *
    * @param raml
    */
  private def parseRamlJsonDocument(basePath: String, raml: JsObject): JsObject = {

    def parseNested(doc: JsValue, currentBasePath: String = basePath): JsValue = {
      doc match {
        case JsInclude(source) =>
          val (newBasePath, included) = RamlToJsonParser.parseToJson(s"$basePath/$source")
          included match {
            case incl: JsObject => parseNested(incl + (Sourced.sourcefield -> JsString(source)), newBasePath)
            case x              => parseNested(x, newBasePath)
          }
        case jsObj: JsObject   =>
          val mappedFields = jsObj.fields.collect {
            case (key, value) => key -> parseNested(value, currentBasePath)
          }
          JsObject(mappedFields)
        case jsArr: JsArray    => JsArray(jsArr.value.map(parseNested(_, currentBasePath)))
        case x                 => x
      }
    }

    parseNested(raml).asInstanceOf[JsObject]
  }


}
