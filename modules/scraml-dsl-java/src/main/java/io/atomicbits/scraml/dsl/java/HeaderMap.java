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

package io.atomicbits.scraml.dsl.java;

import java.util.*;

/**
 * Created by peter on 23/09/15.
 */
public class HeaderMap {

    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private Map<String, String> originalKeys = new HashMap<String, String>();


    public void addHeader(String key, String value) {

        if (key == null || value == null) {
            return;
        }

        String keyOriginal = key.trim();
        String keyLower = keyOriginal.toLowerCase(Locale.ENGLISH);
        String valueOriginal = value.trim();

        if (keyOriginal.isEmpty() || valueOriginal.isEmpty()) {
            return;
        }

        originalKeys.put(keyLower, keyOriginal);
        List<String> currentValues = headers.get(keyLower);
        if (currentValues == null) {
            currentValues = new ArrayList<>();
            headers.put(keyLower, currentValues);
        }
        currentValues.add(valueOriginal);
    }


    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            addHeader(header.getKey(), header.getValue());
        }
    }


    public Map<String, List<String>> getHeaders() {

        Map<String, List<String>> headers = new HashMap<>();

        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            headers.put(originalKeys.get(header.getKey()), header.getValue());
        }

        return headers;
    }


    public HeaderMap cloned() {
        HeaderMap cloned = new HeaderMap();
        cloned.headers = this.cloneHeaders();
        cloned.originalKeys = this.cloneOriginalKeys();
        return cloned;
    }


    public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        String keyLower = key.toLowerCase(Locale.ENGLISH);
        return originalKeys.get(keyLower) != null;
    }

    private <T> List<T> cloneList(List<T> list) {
        List<T> clonedList = new ArrayList<T>(list.size());
        for (T element : list) {
            clonedList.add(element);
        }
        return clonedList;
    }

    private Map<String, String> cloneOriginalKeys() {
        Map<String, String> cloneMap = new HashMap<String, String>();
        cloneMap.putAll(this.originalKeys);
        return cloneMap;
    }

    private Map<String, List<String>> cloneHeaders() {
        Map<String, List<String>> cloneMap = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> header : this.headers.entrySet()) {
            cloneMap.put(header.getKey(), cloneList(header.getValue()));
        }
        return cloneMap;
    }


}
