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

package io.atomicbits.scraml.jdsl.spica;

import java.util.*;

/**
 * Created by peter on 23/09/15.
 */
public class HeaderMap {

    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private Map<String, String> originalKeys = new HashMap<String, String>();


    public void addHeader(String key, String value) {
        List<String> values = new ArrayList<>(1);
        values.add(value);
        addHeader(key, values);
    }


    public void addHeader(String key, List<String> values) {

        if (key == null || values == null) {
            return;
        }

        String keyOriginal = key.trim();
        String keyNormalized = normalizeKey(key);
        List<String> valuesOriginal = new ArrayList<>();
        for (String value : values) {
            if (value != null) valuesOriginal.add(value.trim());
        }

        if (keyOriginal.isEmpty() || valuesOriginal.isEmpty()) {
            return;
        }

        originalKeys.put(keyNormalized, keyOriginal);
        List<String> currentValues = headers.get(keyNormalized);
        if (currentValues == null) {
            currentValues = new ArrayList<>();
            headers.put(keyNormalized, currentValues);
        }
        currentValues.addAll(valuesOriginal);
    }


    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            addHeader(header.getKey(), header.getValue());
        }
    }


    public void addHeaders(HeaderMap headerMap) {
        for (Map.Entry<String, List<String>> header : headerMap.getHeaders().entrySet()) {
            for (String value : header.getValue()) {
                addHeader(header.getKey(), value);
            }
        }
    }


    public void setHeader(String key, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        setHeader(key, values);
    }


    void setHeader(String key, List<String> values) {

        if (key == null || values == null) {
            return;
        }

        String keyOriginal = key.trim();
        String keyNormalized = normalizeKey(key);
        List<String> valuesOriginal = new ArrayList<>();
        for (String value : values) {
            if (value != null) valuesOriginal.add(value.trim());
        }

        if (keyOriginal.isEmpty() || valuesOriginal.isEmpty()) {
            return;
        }

        originalKeys.put(keyNormalized, keyOriginal);
        headers.put(keyNormalized, valuesOriginal);
    }


    public void setHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
    }


    public void setHeaders(HeaderMap headerMap) {
        for (Map.Entry<String, List<String>> header : headerMap.getHeaders().entrySet()) {
            setHeader(header.getKey(), header.getValue());
        }
    }


    public Map<String, List<String>> getHeaders() {

        Map<String, List<String>> headerList = new HashMap<>();

        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            headerList.put(originalKeys.get(header.getKey()), header.getValue());
        }

        return headerList;
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
        String keyNormalized = normalizeKey(key);
        return originalKeys.get(keyNormalized) != null;
    }

    public List<String> getValues(String key) {
        if (key == null) {
            return new ArrayList<>();
        }
        String keyNormalized = normalizeKey(key);
        List<String> values = headers.get(keyNormalized);
        if (values != null) {
            return values;
        } else {
            return new ArrayList<>();
        }
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

    private String normalizeKey(String key) {
        String keyOriginal = key.trim();
        return keyOriginal.toLowerCase(Locale.ENGLISH);
    }

}
