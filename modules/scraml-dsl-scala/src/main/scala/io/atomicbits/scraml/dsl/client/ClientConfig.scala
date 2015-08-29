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

/**
 * Created by peter on 24/08/15. 
 */
case class ClientConfig(requestTimeout: Option[Int] = None,
                        maxRequestRetry: Option[Int] = None,
                        connectTimeout: Option[Int] = None,
                        connectionTTL: Option[Int] = None,
                        readTimeout: Option[Int] = None,
                        webSocketTimeout: Option[Int] = None,
                        maxConnections: Option[Int] = None,
                        maxConnectionsPerHost: Option[Int] = None,
                        allowPoolingConnections: Option[Boolean] = None,
                        allowPoolingSslConnections: Option[Boolean] = None,
                        pooledConnectionIdleTimeout: Option[Int] = None,
                        acceptAnyCertificate: Option[Boolean] = None,
                        followRedirect: Option[Boolean] = None,
                        maxRedirects: Option[Int] = None,
                        removeQueryParamOnRedirect: Option[Boolean] = None,
                        strict302Handling: Option[Boolean] = None)
