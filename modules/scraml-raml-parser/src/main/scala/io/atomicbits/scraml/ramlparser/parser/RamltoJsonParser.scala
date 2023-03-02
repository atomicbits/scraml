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

import java.net.URI

import org.yaml.snakeyaml.Yaml
import play.api.libs.json._

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 10/02/16.
  */
object RamlToJsonParser {

  def parseToJson(source: String): JsonFile = {
    parseToJson(source, "UTF-8")
  }

  def parseToJson(source: String, charsetName: String): JsonFile = {
    Try {
      val SourceFile(path, ramlContent) = SourceReader.read(source, charsetName)
      val ramlContentNoTabs             = ramlContent.replace("\t", "  ") // apparently, the yaml parser does not handle tabs well
      val yaml                          = new Yaml(SimpleRamlConstructor())
      val ramlMap: Any                  = yaml.load(ramlContentNoTabs)

      (path, anyToJson(ramlMap))
    } match {
      case Success((path, jsvalue)) => JsonFile(path, jsvalue)
      case Failure(ex) =>
        ex.printStackTrace()
        sys.error(s"Parsing $source resulted in the following error:\n${ex.getMessage}")
    }
  }

  private def printReadStatus(resourceType: String, resourceOpt: Option[URI]): Any = {
    resourceOpt.map { resource =>
      println(s"Resource found $resourceType: $resource")
    } getOrElse {
      println(s"Resource NOT found $resourceType")
    }
  }

  private def anyToJson(value: Any): JsValue = {
    value match {
      case s: String                      => unwrapJsonString(Json.toJson(s))
      case b: Boolean                     => Json.toJson(b)
      case i: java.lang.Integer           => Json.toJson(i.doubleValue())
      case l: java.lang.Long              => Json.toJson(l.doubleValue())
      case d: Double                      => Json.toJson(d)
      case list: java.util.ArrayList[_] => JsArray(list.asScala.map(anyToJson))
      case map: java.util.Map[_, _] =>
        val mapped =
          map.asScala.map {
            case (field, theValue) => field.toString -> anyToJson(theValue)
          }
        JsObject(mapped.toSeq)
      case include: Include =>
        Json.toJson(include) // the included body is attached in a json object as the string value of the !include field of that JSON object
      case null => JsNull
      case x    => sys.error(s"Cannot parse unknown type $x (${x.getClass.getCanonicalName})")
    }
  }

  /**
    * One time 'unwrap' of a JSON value that is wrapped as a string value.
    */
  private def unwrapJsonString(json: JsValue): JsValue = {
    json match {
      case JsString(stringVal) =>
        Try(Json.parse(stringVal)) match {
          case Success(jsObject: JsObject) =>
            if (jsObject.\("$schema").toOption.isEmpty) {
              jsObject + ("$schema" -> JsString("http://json-schema.org/draft-03/schema"))
            } else {
              jsObject
            }
          case Success(nonStringJsValue) => nonStringJsValue
          case _                         => json
        }
      case _ => json
    }
  }

}
