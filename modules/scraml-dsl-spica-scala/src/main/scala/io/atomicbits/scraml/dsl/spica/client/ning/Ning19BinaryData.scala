/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.spica.client.ning

import java.io.InputStream

import io.atomicbits.scraml.dsl.spica.BinaryData

/**
  * Created by peter on 21/01/16.
  */
class Ning19BinaryData(val innerResponse: com.ning.http.client.Response) extends BinaryData {

  override def asBytes: Array[Byte] = innerResponse.getResponseBodyAsBytes

  override def asString: String = innerResponse.getResponseBody

  override def asString(charset: String): String = innerResponse.getResponseBody(charset)

  /**
    * Request the binary data as a stream. This is convenient when there is a large amount of data to receive.
    * You can only request the input stream once because the data is not stored along the way!
    * Do not close the stream after use.
    *
    * @return An inputstream for reading the binary data.
    */
  override def asStream: InputStream = innerResponse.getResponseBodyAsStream

}
