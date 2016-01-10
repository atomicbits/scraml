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

package io.atomicbits.scraml.dsl.java.client;

/**
 * Created by peter on 10/01/16.
 */
public class FactoryLoader {

    static String defaultClientFactoryClass = "io.atomicbits.scraml.dsl.java.client.ning.Ning19ClientFactory";

    public static ClientFactory load(String clientFactoryClass) {

        String factoryClass = clientFactoryClass != null ? clientFactoryClass : defaultClientFactoryClass;

        try {
            return (ClientFactory) Class.forName(factoryClass).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            String message = e.getMessage() + " The scraml FactoryLoader cannot load factory class " + factoryClass +
                    ". Did you add the dependency to this factory?";
            throw new NoClassDefFoundError(message);
        }
    }

}
