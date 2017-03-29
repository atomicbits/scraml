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

package io.atomicbits.scraml.jdsl;

import io.atomicbits.scraml.jdsl.client.ClientConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by peter on 19/08/15.
 */
public interface Client extends AutoCloseable {

    CompletableFuture<Response<String>> callToStringResponse(RequestBuilder request, String body);

    CompletableFuture<Response<BinaryData>> callToBinaryResponse(RequestBuilder request, String body);

    <R> CompletableFuture<Response<R>> callToTypeResponse(RequestBuilder request, String body, String canonicalResponseType);

    ClientConfig getConfig();

    Map<String, String> getDefaultHeaders();

    String getHost();

    int getPort();

    String getProtocol();

    String getPrefix();

    void close();

}
