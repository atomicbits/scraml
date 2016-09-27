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

package io.atomicbits.scraml.ramlparser.model.types

import io.atomicbits.scraml.ramlparser.model.{Id, ImplicitId, RamlModel, TypeModel}
import play.api.libs.json.{JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

import scala.util.{Success, Try}


/**
  * Created by peter on 26/08/16.
  */
case class FileType(id: Id = ImplicitId,
                    fileTypes: Option[Seq[String]] = None,
                    minLength: Option[Int] = None,
                    maxLength: Option[Int] = None,
                    required: Option[Boolean] = None) extends NonePrimitiveType with AllowedAsObjectField {

  def asRequired = copy(required = Some(true))

  override def updated(updatedId: Id): FileType = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): Type = this

  override def model = RamlModel

}


case object FileType {

  val value = "file"


  def unapply(json: JsValue): Option[Try[FileType]] = {
    Type.typeDeclaration(json).collect {
      case JsString(FileType.value) =>
        Success(
          FileType(
            fileTypes = json.fieldStringListValue("fileTypes"),
            minLength = json.fieldIntValue("minLength"),
            maxLength = json.fieldIntValue("maxLength"),
            required = json.fieldBooleanValue("required")
          )
        )
    }
  }

}
