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

import java.net.{URI, URL}
import java.nio.file.{Files, Paths}
import io.atomicbits.scraml.ramlparser.parser.Include
import org.yaml.snakeyaml.Yaml
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions.mapAsScalaMap
import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
object RamlJsonParser {

  def parseToJson(source: String): JsValue = {
    parseToJson(source, "UTF-8")
  }


  def parseToJson(source: String, charsetName: String): JsValue = {
    Try {
      val ramlContent = read(source, charsetName)
      val yaml = new Yaml(SimpleRamlConstructor())
      val ramlMap: java.util.Map[String, Any] = yaml.load(ramlContent).asInstanceOf[java.util.Map[String, Any]]
      anyToJson(ramlMap)
    } match {
      case Success(jsvalue) => jsvalue
      case Failure(ex)      => sys.error(s"Parsing $source resulted in the following error:\n${ex.getMessage}")
    }
  }


  private def read(source: String, charsetName: String): String = {

    val resource = Try(this.getClass.getResource(source).toURI).toOption
    val file = Try(Paths.get(source)).filter(Files.exists(_)).map(_.toUri).toOption
    val url = Try(new URL(source).toURI).toOption

    val uris: List[Option[URI]] = List(resource, file, url)

    val uri: URI = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $source"))

    val encoded: Array[Byte] = Files.readAllBytes(Paths.get(uri))
    new String(encoded, charsetName) // ToDo: encoding detection via the file's BOM
  }


  private def anyToJson(value: Any): JsValue = {
    value match {
      case s: String                       => Json.toJson(s)
      case b: Boolean                      => Json.toJson(b)
      case i: java.lang.Integer            => Json.toJson(i.doubleValue())
      case d: Double                       => Json.toJson(d)
      case list: java.util.ArrayList[Any]  => JsArray(list.asScala.map(anyToJson))
      case map: java.util.Map[String, Any] => JsObject(mapAsScalaMap(map).mapValues(anyToJson).toSeq)
      case include: Include                => Json.toJson(include)
      case null                            => JsNull
      case x                               => sys.error(s"Cannot parse unknown type $x")
    }
  }

}
