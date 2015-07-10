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

package io.atomicbits.scraml.dsl.support

import java.io.File
import java.nio.charset.Charset

/**
 * Created by peter on 1/07/15. 
 */
sealed trait BodyPart

/**
 *
 * @param name The name of the part.
 * @param bytes The content of the part.
 * @param contentType The optional content type.
 * @param charset The optional character encoding (defaults to UTF-8).
 * @param contentId The optional content id.
 * @param transferEncoding The optional transfer encoding.
 */
case class ByteArrayPart(name: String,
                         bytes: Array[Byte],
                         contentType: Option[String] = None,
                         charset: Option[Charset] = Some(Charset.forName("UTF8")),
                         contentId: Option[String] = None,
                         transferEncoding: Option[String] = None) extends BodyPart

/**
 *
 * @param name The name of the part.
 * @param file The file.
 * @param fileName The optional name of the file, if no name is given the name in 'file' is used.
 * @param contentType The optional content type.
 * @param charset The optional character encoding (defaults to UTF-8).
 * @param contentId The optional content id.
 * @param transferEncoding The optional transfer encoding.
 */
case class FilePart(name: String,
                    file: File,
                    fileName: Option[String] = None,
                    contentType: Option[String] = None,
                    charset: Option[Charset] = Some(Charset.forName("UTF8")),
                    contentId: Option[String] = None,
                    transferEncoding: Option[String] = None) extends BodyPart

/**
 *
 * @param name The name of the part.
 * @param value The content of the part.
 * @param contentType The optional content type.
 * @param charset The optional character encoding (defaults to UTF-8).
 * @param contentId The optional content id.
 * @param transferEncoding The optional transfer encoding.
 */
case class StringPart(name: String,
                      value: String,
                      contentType: Option[String] = None,
                      charset: Option[Charset] = Some(Charset.forName("UTF8")),
                      contentId: Option[String] = None,
                      transferEncoding: Option[String] = None) extends BodyPart
