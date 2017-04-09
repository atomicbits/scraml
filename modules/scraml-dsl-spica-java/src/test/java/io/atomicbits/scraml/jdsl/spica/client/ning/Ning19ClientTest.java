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

package io.atomicbits.scraml.jdsl.spica.client.ning;

import io.atomicbits.scraml.jdsl.spica.client.ClientConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Created by peter on 22/04/16.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class Ning19ClientTest {

    @Test
    public void testFetchCharsetFromHeaders() {
        Ning19Client client = new Ning19Client("localhost", 8080, "http", null, new ClientConfig(), null);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> acceptValues = new ArrayList<>();
        acceptValues.add("application/json");
        acceptValues.add("application/bson");
        headers.put("Accept", acceptValues);
        List<String> contentTypeValues = new ArrayList<>();
        contentTypeValues.add("application/json;charset=UTF-8");
        headers.put("Content-Type", contentTypeValues);

        assertEquals("UTF-8", client.getResponseCharsetFromHeaders(headers, "ascii"));
    }

    @Test
    public void testFetchCharsetFromHeadersSpace() {
        Ning19Client client = new Ning19Client("localhost", 8080, "http", null, new ClientConfig(), null);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> acceptValues = new ArrayList<>();
        acceptValues.add("application/json");
        acceptValues.add("application/bson");
        headers.put("Accept", acceptValues);
        List<String> contentTypeValues = new ArrayList<>();
        contentTypeValues.add("application/json; charset=UTF-8");
        headers.put("Content-type", contentTypeValues);

        assertEquals("UTF-8", client.getResponseCharsetFromHeaders(headers, "ascii"));
    }

    @Test
    public void testFetchCharsetFromHeadersDefault() {
        Ning19Client client = new Ning19Client("localhost", 8080, "http", null, new ClientConfig(), null);

        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> acceptValues = new ArrayList<>();
        acceptValues.add("application/json");
        acceptValues.add("application/bson");
        headers.put("Accept", acceptValues);
        List<String> contentTypeValues = new ArrayList<>();
        contentTypeValues.add("application/json");
        headers.put("Content-type", contentTypeValues);

        assertEquals("ascii", client.getResponseCharsetFromHeaders(headers, "ascii"));
    }

}
