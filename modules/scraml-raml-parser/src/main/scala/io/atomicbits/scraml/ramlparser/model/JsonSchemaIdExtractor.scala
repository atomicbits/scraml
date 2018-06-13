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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedTypeReference
import play.api.libs.json.{ JsObject, JsValue }

/**
  * Created by peter on 25/03/16.
  */
object JsonSchemaIdExtractor {

  def apply(json: JsValue): Id =
    List(JsonSchemaIdAnalyser.idFromField(json, "id"), JsonSchemaIdAnalyser.idFromField(json, "title")).flatten.headOption
      .getOrElse(ImplicitId)

}

object RefExtractor {

  def unapply(json: JsValue): Option[Id] = JsonSchemaIdAnalyser.idFromField(json, ParsedTypeReference.value)

}

object JsonSchemaIdAnalyser {

  /**
    * Transform the given field of the schema to an Id if possible.
    *
    * @param json The schema
    * @param field The id field
    * @return The Id
    */
  def idFromField(json: JsValue, field: String): Option[Id] =
    (json \ field).asOpt[String] map idFromString

  def idFromString(id: String): Id = {
    if (isRoot(id)) RootId(id = cleanRoot(id))
    else if (isFragment(id)) idFromFragment(id)
    else if (isAbsoluteFragment(id)) idFromAbsoluteFragment(id)
    else RelativeId(id = id.trim.stripPrefix("/"))
  }

  def isRoot(id: String): Boolean = id.contains("://") && !isAbsoluteFragment(id)

  def isFragment(id: String): Boolean = {
    id.trim.startsWith("#")
  }

  def idFromFragment(id: String): FragmentId = {
    FragmentId(id.trim.stripPrefix("#").stripPrefix("/").split('/').toList)
  }

  def isAbsoluteFragment(id: String): Boolean = {
    val parts = id.trim.split('#')
    parts.length == 2 && parts(0).contains("://")
  }

  def idFromAbsoluteFragment(id: String): AbsoluteFragmentId = {
    val parts = id.trim.split('#')
    AbsoluteFragmentId(RootId(parts(0)), parts(1).split('/').toList.collect { case part if part.nonEmpty => part })
  }

  def cleanRoot(root: String): String = {
    root.trim.stripSuffix("#")
  }

  def isModelObject(json: JsObject): Boolean = {

    (json \ "type").asOpt[String].exists(_ == "object")

  }

}
