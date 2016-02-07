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

import java.io.{FileReader, StringReader, File}
import java.nio.file.{Paths, Files}

import io.atomicbits.scraml.ramlparser.model.Include
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import play.api.libs.json.Json.JsValueWrapper

import play.api.libs.json._
import play.api.libs.json.Json._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.JavaConversions.mapAsScalaMap

/**
  * Created by peter on 6/02/16.
  */
object RamlParser {

  def run: JsValue = {
    val encoded: Array[Byte] = Files.readAllBytes(Paths.get(this.getClass.getResource("/test001.raml").toURI))
    val ramlContent = new String(encoded, "UTF-8")
    val yaml = new Yaml(SimpleRamlConstructor())
    val ramlMap: java.util.Map[String, Any] = yaml.load(ramlContent).asInstanceOf[java.util.Map[String, Any]]
    mapToJson(ramlMap)
  }


  /**
    * types:
    * String
    * Boolean
    * Integer
    * Double
    * ArrayList
    * LinkedHashMap
    * Include
    *
    */


  def mapToJson(ramlMap: java.util.Map[String, Any]): JsValue = {

    def anyToJson(value: Any): JsValueWrapper = {
      value match {
        case s: String                       => Json.toJson(s)
        case b: Boolean                      => Json.toJson(b)
        case i: java.lang.Integer            => Json.toJson(i.doubleValue())
        case d: Double                       => Json.toJson(d)
        case list: java.util.ArrayList[Any]  => Json.arr(list.asScala.map(anyToJson): _*)
        case map: java.util.Map[String, Any] => mapToJson(map)
        case include: Include                => Json.toJson(include)
        case null                            => JsNull
        case x                               => sys.error(s"Cannot parse unknown type $x")
      }
    }

    Json.obj(mapAsScalaMap(ramlMap).mapValues(anyToJson).toSeq: _*)
  }

}
