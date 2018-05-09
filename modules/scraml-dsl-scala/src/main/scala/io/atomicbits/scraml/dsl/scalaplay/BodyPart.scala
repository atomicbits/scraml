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

package io.atomicbits.scraml.dsl.scalaplay

import _root_.java.io.File
import _root_.java.nio.charset.Charset

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
                         contentType: Option[String]      = None,
                         charset: Option[Charset]         = Some(Charset.forName("UTF8")),
                         contentId: Option[String]        = None,
                         transferEncoding: Option[String] = None)
    extends BodyPart

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
                    fileName: Option[String]         = None,
                    contentType: Option[String]      = None,
                    charset: Option[Charset]         = Some(Charset.forName("UTF8")),
                    contentId: Option[String]        = None,
                    transferEncoding: Option[String] = None)
    extends BodyPart

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
                      contentType: Option[String]      = None,
                      charset: Option[Charset]         = Some(Charset.forName("UTF8")),
                      contentId: Option[String]        = None,
                      transferEncoding: Option[String] = None)
    extends BodyPart
