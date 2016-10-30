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
import org.yaml.snakeyaml.Yaml
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions.mapAsScalaMap
import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
object RamlToJsonParser {

  def parseToJson(source: String): (String, JsValue) = {
    parseToJson(source, "UTF-8")
  }


  def parseToJson(source: String, charsetName: String): (String, JsValue) = {
    Try {
      val (path, ramlContent) = read(source, charsetName)
      val ramlContentNoTabs = ramlContent.replace("\t", "  ") // apparently, the yaml parser does not handle tabs well
      val yaml = new Yaml(SimpleRamlConstructor())
      val ramlMap: Any = {
        val yamled = yaml.load(ramlContentNoTabs)
        Try(yamled.asInstanceOf[java.util.Map[Any, Any]]).getOrElse(yamled.asInstanceOf[String])
      }
      (path, anyToJson(ramlMap))
    } match {
      case Success((path: String, jsvalue)) => (path, jsvalue)
      case Failure(ex)                      => sys.error(s"Parsing $source resulted in the following error:\n${ex.getMessage}")
    }
  }


  private def read(source: String, charsetName: String): (String, String) = {

    /**
      * Beware, Windows paths are represented as URL as follows: file:///C:/Users/someone
      * uri.normalize().getPath then gives /C:/Users/someone instead of C:/Users/someone
      * Paths.get(uri) is fine, but if we want to work with the URI as a string representation,
      * then we need to strip off the leading '/'.
      * see: http://stackoverflow.com/questions/18520972/converting-java-file-url-to-file-path-platform-independent-including-u
      * and: http://stackoverflow.com/questions/9834776/java-nio-file-path-issue
      */
    def cleanWindowsTripleSlashIssue(path: String): String = {
      val hasWindowsPrefix =
        path.split('/').filter(_.nonEmpty).headOption.collect {
          case first if first.endsWith(":") => true
        } getOrElse false
      if (hasWindowsPrefix && path.startsWith("/")) path.drop(1)
      else path
    }
    
    val resource = Try(this.getClass.getResource(source).toURI).toOption
    val file = Try(Paths.get(source)).filter(Files.exists(_)).map(_.toUri).toOption
    val url = Try(new URL(source).toURI).toOption

    val uris: List[Option[URI]] = List(resource, file, url)

    val uri: URI = uris.flatten.headOption.getOrElse(sys.error(s"Unable to find resource $source"))

    val path = uri.normalize().getPath.split("/").dropRight(1).mkString("/")
    val encoded: Array[Byte] = Files.readAllBytes(Paths.get(uri))
    (path, new String(encoded, charsetName)) // ToDo: encoding detection via the file's BOM
  }


  private def anyToJson(value: Any): JsValue = {
    value match {
      case s: String                      => unwrapJsonString(Json.toJson(s))
      case b: Boolean                     => Json.toJson(b)
      case i: java.lang.Integer           => Json.toJson(i.doubleValue())
      case d: Double                      => Json.toJson(d)
      case list: java.util.ArrayList[Any] => JsArray(list.asScala.map(anyToJson))
      case map: java.util.Map[Any, Any]   =>
        val mapped =
          mapAsScalaMap(map).map {
            case (field, theValue) => field.toString -> anyToJson(theValue)
          }
        JsObject(mapped.toSeq)
      case include: Include               => Json.toJson(include)
      case null                           => JsNull
      case x                              => sys.error(s"Cannot parse unknown type $x")
    }
  }

  /**
    * One time 'unwrap' of a JSON value that is wrapped as a string value.
    */
  private def unwrapJsonString(json: JsValue): JsValue = {
    json match {
      case JsString(stringVal) =>
        Try(Json.parse(stringVal)) match {
          case Success(nonStringJsValue) => nonStringJsValue
          case _                         => json
        }
      case _                   => json
    }
  }

}
