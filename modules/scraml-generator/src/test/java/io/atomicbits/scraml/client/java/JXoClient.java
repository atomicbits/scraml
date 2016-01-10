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

package io.atomicbits.scraml.client.java;

import io.atomicbits.scraml.dsl.java.Client;
import io.atomicbits.scraml.dsl.java.RequestBuilder;
import io.atomicbits.scraml.dsl.java.client.ClientConfig;
import io.atomicbits.scraml.dsl.java.client.FactoryLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 19/08/15.
 */
public class JXoClient {

    private String host;
    private int port;
    private String protocol;
    private Map<String, String> defaultHeaders;

    // It's important that the requestBuilder is package-accessible so that it's not visible in the DSL.
    RequestBuilder requestBuilder = new RequestBuilder();


    public JXoClient(String host,
                     int port,
                     String protocol,
                     String prefix,
                     ClientConfig clientConfig,
                     Map<String, String> defaultHeaders) {
        this(host, port, protocol, prefix, clientConfig, defaultHeaders, null);
    }

    public JXoClient(String host,
                     int port,
                     String protocol,
                     String prefix,
                     ClientConfig clientConfig,
                     Map<String, String> defaultHeaders,
                     String clientFactory) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.defaultHeaders = defaultHeaders;

        // Have a look at how the field 'rest' is initialized. That's why we have to reuse the existing (empty) RequestBuilder.
        Client client = FactoryLoader.load(clientFactory).createClient(host, port, protocol, prefix, clientConfig, defaultHeaders);
        this.requestBuilder.setClient(client);
        this.requestBuilder.initializeChildren();
        System.out.println(this.requestBuilder.toString());
    }


    public RestResource rest = new RestResource(this.requestBuilder);


    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void close() {
        this.requestBuilder.getClient().close();
    }

}
