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
