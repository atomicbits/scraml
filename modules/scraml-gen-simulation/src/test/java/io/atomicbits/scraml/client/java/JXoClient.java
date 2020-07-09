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

package io.atomicbits.scraml.client.java;

import io.atomicbits.scraml.dsl.javajackson.Client;
import io.atomicbits.scraml.dsl.javajackson.RequestBuilder;
import io.atomicbits.scraml.dsl.javajackson.client.ClientConfig;
import io.atomicbits.scraml.dsl.javajackson.client.ClientFactory;
import io.atomicbits.scraml.dsl.javajackson.client.ning.Ning2ClientFactory;

import java.util.Map;

/**
 * Created by peter on 19/08/15.
 */
public class JXoClient {

    // It's important that the requestBuilder is package-accessible so that it's not visible in the DSL.
    protected RequestBuilder _requestBuilder = new RequestBuilder();

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
                     ClientFactory clientFactory) {
        ClientFactory cFactory = clientFactory != null ? clientFactory : new Ning2ClientFactory();
        Client client = cFactory.createClient(host, port, protocol, prefix, clientConfig, defaultHeaders);
        this._requestBuilder.setClient(client);
        System.out.println(this._requestBuilder.toString());
    }

    public RestResource rest = new RestResource(this._requestBuilder);

    public void _close() {
        try {
            this._requestBuilder.getClient().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
