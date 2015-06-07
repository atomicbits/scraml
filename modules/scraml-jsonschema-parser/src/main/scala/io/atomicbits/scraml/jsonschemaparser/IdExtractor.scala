/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object IdExtractor {

  def unapply(schema: JsObject): Option[Id] = IdAnalyser.idFromField(schema, "id")

}

object RefExtractor {

  def unapply(schema: JsObject): Option[Id] = IdAnalyser.idFromField(schema, "$ref")

}

object IdAnalyser {

  def idFromField(schema: JsObject, field: String): Option[Id] = {
    val idType = (schema \ field).asOpt[String] match {
      case Some(id) =>
        if (isRoot(id)) AbsoluteId(id = cleanRoot(id))
        else if (isFragment(id)) FragmentId(id)
        else if (isAbsoluteFragment(id)) AbsoluteFragmentId(id)
        else RelativeId(id = id.trim.stripPrefix("/"))
      case None => ImplicitId
    }

    Option(idType)
  }

  def isRoot(id: String): Boolean = id.contains("://")

  def isFragment(id: String): Boolean = {
    id.trim.startsWith("#")
  }

  def isAbsoluteFragment(id: String): Boolean = {
    val parts = id.trim.split("#")
    parts.length == 2 && parts(0).contains("://")
  }

  def cleanRoot(root: String): String = {
    root.trim.stripSuffix("#")
  }

  def isModelObject(schema: JsObject): Boolean = {

    (schema \ "type").asOpt[String].contains("object")
    // && (schema \ "properties").isInstanceOf[JsObject]
    // --> the above is unsafe because an object may not have a properties field, but a oneOf instead

  }

}


