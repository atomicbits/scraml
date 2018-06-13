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

package io.atomicbits.scraml.dsl.javajackson.client.ning;

import io.atomicbits.scraml.dsl.javajackson.client.ClientConfig;
import io.atomicbits.scraml.dsl.javajackson.client.ClientFactory;
import io.atomicbits.scraml.dsl.javajackson.Client;

import java.util.Map;

/**
 * Created by peter on 10/01/16.
 */
public class Ning19ClientFactory implements ClientFactory {

    @Override
    public Client createClient(String host,
                               Integer port,
                               String protocol,
                               String prefix,
                               ClientConfig config,
                               Map<String, String> defaultHeaders) {
        try {
            return new Ning19Client(host, port, protocol, prefix, config, defaultHeaders);
        } catch (NoClassDefFoundError e) {
            String message = e.getMessage() +
                    " The Scraml ning client factory cannot find the necessary ning dependencies to instantiate its client. " +
                    "Did you add the necessary ning dependencies to your project?";
            throw new NoClassDefFoundError(message);
        }
    }

}
