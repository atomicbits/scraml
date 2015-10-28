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

import com.ning.http.client.AsyncHttpClientConfigDefaults

/**
 * Created by peter on 24/08/15. 
 */
case class ClientConfig(requestTimeout: Int = AsyncHttpClientConfigDefaults.defaultRequestTimeout(),
                        maxRequestRetry: Int = AsyncHttpClientConfigDefaults.defaultMaxRequestRetry(),
                        connectTimeout: Int = AsyncHttpClientConfigDefaults.defaultConnectTimeout(),
                        connectionTTL: Int = AsyncHttpClientConfigDefaults.defaultConnectionTTL(),
                        readTimeout: Int = AsyncHttpClientConfigDefaults.defaultReadTimeout(),
                        webSocketTimeout: Int = AsyncHttpClientConfigDefaults.defaultWebSocketTimeout(),
                        maxConnections: Int = AsyncHttpClientConfigDefaults.defaultMaxConnections(),
                        maxConnectionsPerHost: Int = AsyncHttpClientConfigDefaults.defaultMaxConnectionsPerHost(),
                        allowPoolingConnections: Boolean = AsyncHttpClientConfigDefaults.defaultAllowPoolingConnections(),
                        allowPoolingSslConnections: Boolean = AsyncHttpClientConfigDefaults.defaultAllowPoolingSslConnections(),
                        pooledConnectionIdleTimeout: Int = AsyncHttpClientConfigDefaults.defaultPooledConnectionIdleTimeout(),
                        acceptAnyCertificate: Boolean = AsyncHttpClientConfigDefaults.defaultAcceptAnyCertificate(),
                        followRedirect: Boolean = AsyncHttpClientConfigDefaults.defaultFollowRedirect(),
                        maxRedirects: Int = AsyncHttpClientConfigDefaults.defaultMaxRedirects(),
                        removeQueryParamOnRedirect: Boolean = AsyncHttpClientConfigDefaults.defaultRemoveQueryParamOnRedirect(),
                        strict302Handling: Boolean = AsyncHttpClientConfigDefaults.defaultStrict302Handling(),
                        responseCharset: Charset = Charset.defaultCharset)
