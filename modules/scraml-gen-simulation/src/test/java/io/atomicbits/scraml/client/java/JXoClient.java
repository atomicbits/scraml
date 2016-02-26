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

import io.atomicbits.scraml.jdsl.Client;
import io.atomicbits.scraml.jdsl.RequestBuilder;
import io.atomicbits.scraml.jdsl.client.ClientConfig;
import io.atomicbits.scraml.jdsl.client.ClientFactory;
import io.atomicbits.scraml.jdsl.client.ning.Ning19ClientFactory;

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
        ClientFactory cFactory = clientFactory != null ? clientFactory : new Ning19ClientFactory();
        Client client = cFactory.createClient(host, port, protocol, prefix, clientConfig, defaultHeaders);
        this._requestBuilder.setClient(client);
        System.out.println(this._requestBuilder.toString());
    }

    public RestResource rest = new RestResource(this._requestBuilder);

    public void _close() {
        this._requestBuilder.getClient().close();
    }

}
