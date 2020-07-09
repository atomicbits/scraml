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

package io.atomicbits.scraml.dsl.scalaplay.client

import java.nio.charset.Charset

/**
  * Created by peter on 24/08/15.
  *
  * Time is in ms.
  */
case class ClientConfig(requestTimeout: Int                 = 60 * 1000,
                        maxRequestRetry: Int                = 5,
                        connectTimeout: Int                 = 5 * 1000,
                        connectionTTL: Int                  = -1,
                        readTimeout: Int                    = 60 * 1000,
                        webSocketTimeout: Int               = 15 * 60 * 1000,
                        maxConnections: Int                 = -1,
                        maxConnectionsPerHost: Int          = -1,
                        allowPoolingConnections: Boolean    = true,
                        allowPoolingSslConnections: Boolean = true,
                        pooledConnectionIdleTimeout: Int    = 60 * 1000,
                        useInsecureTrustManager: Boolean    = false,
                        followRedirect: Boolean             = false,
                        maxRedirects: Int                   = 5,
                        strict302Handling: Boolean          = false,
                        responseCharset: Charset            = Charset.defaultCharset(),
                        requestCharset: Charset             = Charset.defaultCharset())
