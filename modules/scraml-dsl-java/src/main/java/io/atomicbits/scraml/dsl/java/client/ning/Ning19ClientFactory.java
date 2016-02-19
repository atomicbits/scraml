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

package io.atomicbits.scraml.dsl.java.client.ning;

import io.atomicbits.scraml.dsl.java.Client;
import io.atomicbits.scraml.dsl.java.NonNull;
import io.atomicbits.scraml.dsl.java.client.ClientConfig;
import io.atomicbits.scraml.dsl.java.client.ClientFactory;

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
