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

package io.atomicbits.scraml.dsl.client

import java.nio.charset.Charset

/**
 * Created by peter on 24/08/15.
 *
 * Time is in ms.
 */
case class ClientConfig(requestTimeout: Int = 60 * 1000,
                        maxRequestRetry: Int = 5,
                        connectTimeout: Int = 5 * 1000,
                        connectionTTL: Int = -1,
                        readTimeout: Int = 60 * 1000,
                        webSocketTimeout: Int = 15 * 60 * 1000,
                        maxConnections: Int = -1,
                        maxConnectionsPerHost: Int = -1,
                        allowPoolingConnections: Boolean = true,
                        allowPoolingSslConnections: Boolean = true,
                        pooledConnectionIdleTimeout: Int = 60 * 1000,
                        acceptAnyCertificate: Boolean = false,
                        followRedirect: Boolean = false,
                        maxRedirects: Int = 5,
                        strict302Handling: Boolean = false,
                        responseCharset: Charset = Charset.defaultCharset(),
                        requestCharset: Charset = Charset.defaultCharset())
