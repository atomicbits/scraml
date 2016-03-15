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


import io.atomicbits.scraml.ramlparser.model.{JsInclude, Raml}
import play.api.libs.json._


/**
  * Created by peter on 6/02/16.
  */
case class RamlParser(ramlSource: String, charsetName: String) {


  def parse = {
    val ramlJson = RamlJsonParser.parseToJson(ramlSource, charsetName)
    val parsed =
      ramlJson match {
        case ramlJsObj: JsObject => parseRamlJsonDocument(ramlJsObj)
        case x                   => sys.error(s"Could not parse $ramlSource, expected a RAML document.")
      }

    implicit val parseContext = ParseContext(List(ramlSource))

    Raml(parsed)
  }

  /**
    * Recursively parse all RAML documents by following all include statements and packing everything in one big JSON object.
    * The source reference will be injected under the "_source" field so that we can trace the origin al all documents later on.
    *
    * @param raml
    */
  private def parseRamlJsonDocument(raml: JsObject): JsObject = {

    def parseNested(doc: JsValue): JsValue = {
      doc match {
        case JsInclude(included, source) => parseNested(included + (Sourced.sourcefield -> JsString(source)))
        case jsObj: JsObject             =>
          val mappedFields = jsObj.fields.collect {
            case (key, value) => key -> parseNested(value)
          }
          JsObject(mappedFields)
        case jsArr: JsArray              => JsArray(jsArr.value.map(parseNested))
        case x                           => x
      }
    }

    parseNested(raml).asInstanceOf[JsObject]
  }


}
