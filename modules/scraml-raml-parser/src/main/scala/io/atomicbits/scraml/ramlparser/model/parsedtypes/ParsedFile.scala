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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model.{ Id, ImplicitId, RamlModel, TypeModel }
import play.api.libs.json.{ JsString, JsValue }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.util.{ Success, Try }

/**
  * Created by peter on 26/08/16.
  */
case class ParsedFile(id: Id                         = ImplicitId,
                      fileTypes: Option[Seq[String]] = None,
                      minLength: Option[Int]         = None,
                      maxLength: Option[Int]         = None,
                      required: Option[Boolean]      = None)
    extends NonPrimitiveType
    with AllowedAsObjectField {

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): ParsedFile = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = this

  override def model = RamlModel

}

case object ParsedFile {

  val value = "file"

  def unapply(json: JsValue): Option[Try[ParsedFile]] = {
    ParsedType.typeDeclaration(json).collect {
      case JsString(ParsedFile.value) =>
        Success(
          ParsedFile(
            fileTypes = json.fieldStringListValue("fileTypes"),
            minLength = json.fieldIntValue("minLength"),
            maxLength = json.fieldIntValue("maxLength"),
            required  = json.fieldBooleanValue("required")
          )
        )
    }
  }

}
