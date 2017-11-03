/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson.client.okhttp;

import io.atomicbits.scraml.dsl.androidjavajackson.BinaryData;
import io.atomicbits.scraml.dsl.androidjavajackson.Client;
import io.atomicbits.scraml.dsl.androidjavajackson.RequestBuilder;
import io.atomicbits.scraml.dsl.androidjavajackson.Response;
import io.atomicbits.scraml.dsl.androidjavajackson.client.ClientConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by peter on 3/11/17.
 */
public class OkHttpScramlClient implements Client {

    public OkHttpScramlClient(String host,
                              Integer port,
                              String protocol,
                              String prefix,
                              ClientConfig config,
                              Map<String, String> defaultHeaders) {
    }

    @Override
    public CompletableFuture<Response<String>> callToStringResponse(RequestBuilder request, String body) {
        return null;
    }

    @Override
    public CompletableFuture<Response<BinaryData>> callToBinaryResponse(RequestBuilder request, String body) {
        return null;
    }

    @Override
    public <R> CompletableFuture<Response<R>> callToTypeResponse(RequestBuilder request, String body, String canonicalResponseType) {
        return null;
    }

    @Override
    public ClientConfig getConfig() {
        return null;
    }

    @Override
    public Map<String, String> getDefaultHeaders() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public void close() {

    }
}
