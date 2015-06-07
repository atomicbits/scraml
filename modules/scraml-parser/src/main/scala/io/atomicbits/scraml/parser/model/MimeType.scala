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

package io.atomicbits.scraml.parser.model

import scala.collection.JavaConverters._

import java.util.{List => JList}

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io).
 *
 * It is not clear why formParameters is a Map from String to a List of FormParameters in the original Java model,
 * I would expect a Map from String to a signle FormParameter.
 *
 */
case class MimeType(mimeType: String, schema: Option[String], formParameters: Map[String, List[Parameter]])

object MimeType {

  def apply(mimeType: org.raml.model.MimeType): MimeType = {

    val formParameters: Map[String, List[Parameter]] =
      Transformer
        .transformMap[JList[org.raml.model.parameter.FormParameter], List[Parameter]] { formParamList =>
        formParamList.asScala.toList.map(Parameter(_))
      }(mimeType.getFormParameters)


    MimeType(mimeType.getType, Option(mimeType.getSchema), formParameters)
  }

}
