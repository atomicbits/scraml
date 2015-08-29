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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by peter on 19/08/15.
 */
public class RequestBuilder {

    Client client;
    List<String> path = new ArrayList<String>();
    Method method = Method.GET;
    Map<String, HttpParam> queryParameters = new HashMap<String, HttpParam>();
    Map<String, HttpParam> formParameters = new HashMap<String, HttpParam>();
    List<BodyPart> multipartParams = new ArrayList<BodyPart>();
    List<String> validAcceptHeaders = new ArrayList<String>();
    List<String> validContentTypeHeaders = new ArrayList<String>();
    Map<String, String> headers = new HashMap<String, String>();

    // Java makes it hard for us to get the initialization of the requestbuilders right.
    // We need to do some 'reverse initialization' in order to work with fields instead of methods to point
    // to our REST path segments.
    List<RequestBuilder> childRequestBuilders = new ArrayList<RequestBuilder>();

    public RequestBuilder() {
    }

    public RequestBuilder(Client client) {
        this.client = client;
    }

    private RequestBuilder(Client client,
                           Map<String, HttpParam> formParameters,
                           Map<String, String> headers,
                           Method method,
                           List<BodyPart> multipartParams,
                           Map<String, HttpParam> queryParameters,
                           List<String> path,
                           List<String> validAcceptHeaders,
                           List<String> validContentTypeHeaders) {
        this.client = client;
        this.formParameters = formParameters;
        this.headers = headers;
        this.method = method;
        this.multipartParams = multipartParams;
        this.queryParameters = queryParameters;
        this.path = path;
        this.validAcceptHeaders = validAcceptHeaders;
        this.validContentTypeHeaders = validContentTypeHeaders;
    }

    public Client getClient() {
        return client;
    }

    public void appendPathElement(String pathElement) {
        this.path.add(pathElement);
    }

    public void prependPathElements(List<String> pathElements) {
        this.path.addAll(0, pathElements);
    }

    public RequestBuilder cloneAddHeader(String key, String value) {
        RequestBuilder clone = this.shallowClone();
        Map<String, String> clonedHeaders = cloneMap(clone.headers);
        clonedHeaders.put(key, value);
        clone.headers = clonedHeaders;
        return clone;
    }

    public <B, R> Future<Response<R>> callToTypeResponse(B body) {
        return client.callToTypeResponse(this, body);
    }

    public RequestBuilder shallowClone() {
        return new RequestBuilder(
                this.client,
                this.formParameters,
                this.headers,
                this.method,
                this.multipartParams,
                this.queryParameters,
                this.path,
                this.validAcceptHeaders,
                this.validContentTypeHeaders
        );
    }

    /**
     * Initialize this with a given requestbuilder.
     *
     * @param requestBuilder The requestbuilder to initialize this with.
     */
    public void initialize(RequestBuilder requestBuilder) {
        this.client = requestBuilder.client;
        this.formParameters = requestBuilder.formParameters;
        this.headers = requestBuilder.headers;
        this.method = requestBuilder.method;
        this.multipartParams = requestBuilder.multipartParams;
        this.queryParameters = requestBuilder.queryParameters;
        this.path = requestBuilder.path;
        this.validAcceptHeaders = requestBuilder.validAcceptHeaders;
        this.validContentTypeHeaders = requestBuilder.validContentTypeHeaders;
    }

    private <T> List<T> cloneList(List<T> list) {
        List<T> clonedList = new ArrayList<T>(list.size());
        for (T element : list) {
            clonedList.add(element);
        }
        return clonedList;
    }

    private <T> Map<String, T> cloneMap(Map<String, T> map) {
        Map<String, T> cloneMap = new HashMap<String, T>();
        cloneMap.putAll(map);
        return cloneMap;
    }

    public void addChild(RequestBuilder requestBuilder) {
        childRequestBuilders.add(requestBuilder);
    }

    public void initializeChildren() {
        for (RequestBuilder child : childRequestBuilders) {
            initializeChild(this, child);
        }
    }

    private void initializeChild(RequestBuilder parent, RequestBuilder child) {
        child.client = parent.client;
        child.prependPathElements(parent.path);
        child.initializeChildren();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("client:\t\t" + client + "\n");
        sb.append("path:\t\t" + listToString(path) + "\n");
        for (RequestBuilder child : childRequestBuilders) {
            sb.append(child.toString());
        }
        return sb.toString();
    }

    private String listToString(List list) {
        String txt = "";
        for (Object o : list) {
            txt += o.toString() + ", ";
        }
        return txt;
    }

}
