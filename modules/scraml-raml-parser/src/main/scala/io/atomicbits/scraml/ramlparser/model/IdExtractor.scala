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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedTypeReference
import play.api.libs.json.{JsObject, JsValue}

/**
  * Created by peter on 25/03/16.
  */
object IdExtractor {

  def unapply(schema: JsValue): Option[Id] = IdAnalyser.idFromField(schema, "id")

}

object RefExtractor {

  def unapply(schema: JsValue): Option[Id] = IdAnalyser.idFromField(schema, ParsedTypeReference.value)

}

object IdAnalyser {

  /**
    * Transform the given field of the schema to an Id if possible.
    *
    * @param schema The schema
    * @param field The id field
    * @return The Id
    */
  def idFromField(schema: JsValue, field: String): Option[Id] = {

    val idType = (schema \ field).asOpt[String] match {
      case Some(idOrRef) => idFromString(idOrRef)
      case None => ImplicitId
    }

    Option(idType)
  }

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

  def isModelObject(schema: JsObject): Boolean = {

    (schema \ "type").asOpt[String].exists(_ == "object")

  }

}


