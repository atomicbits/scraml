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

import io.atomicbits.scraml.dsl.androidjavajackson.Client;
import io.atomicbits.scraml.dsl.androidjavajackson.client.ClientConfig;
import io.atomicbits.scraml.dsl.androidjavajackson.client.ClientFactory;

import java.util.Map;

/**
 * Created by peter on 3/11/17.
 */
public class OkHttpScramlClientFactory implements ClientFactory {

    @Override
    public Client createClient(String host,
                               Integer port,
                               String protocol,
                               String prefix,
                               ClientConfig config,
                               Map<String, String> defaultHeaders) {
        try {
            return new OkHttpScramlClient(host, port, protocol, prefix, config, defaultHeaders);
        } catch (NoClassDefFoundError e) {
            String message = e.getMessage() +
                    " The Scraml http client factory cannot find the necessary dependencies to instantiate its client. " +
                    "Did you add the necessary OkHttp dependencies to your project?";
            throw new NoClassDefFoundError(message);
        }
    }

}
