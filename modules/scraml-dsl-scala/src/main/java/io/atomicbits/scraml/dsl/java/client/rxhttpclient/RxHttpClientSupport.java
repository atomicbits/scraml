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

package io.atomicbits.scraml.dsl.java.client.rxhttpclient;

import io.atomicbits.scraml.dsl.java.Client;
import io.atomicbits.scraml.dsl.java.RequestBuilder;
import io.atomicbits.scraml.dsl.java.Response;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by peter on 19/08/15.
 */
public class RxHttpClientSupport implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private int requestTimeout;
    private int maxConnections;
    private Map<String, String> defaultHeaders;

    public RxHttpClientSupport(String protocol, String host, int port, String prefix, int requestTimeout, int maxConnections, Map<String, String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        this.host = host;
        this.maxConnections = maxConnections;
        this.port = port;
        this.prefix = prefix;
        this.protocol = protocol;
        this.requestTimeout = requestTimeout;
    }


    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public String getHost() {
        return host;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getPort() {
        return port;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public Map<String, String> defaultHeaders() {
        return null;
    }

    @Override
    public <B> Future<Response<String>> callToStringResponse(RequestBuilder request, B body) {
        return null;
    }

    @Override
    public <B, R> Future<Response<R>> callToTypeResponse(RequestBuilder request, B body) {
        return null;
    }

    @Override
    public void close() {

    }

}
