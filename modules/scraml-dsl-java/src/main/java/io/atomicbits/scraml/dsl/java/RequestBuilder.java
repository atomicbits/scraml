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

import io.atomicbits.scraml.dsl.java.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by peter on 19/08/15.
 */
public class RequestBuilder {

    private Client client;
    private List<String> path = new ArrayList<String>();
    private Method method = Method.GET;
    private Map<String, HttpParam> queryParameters = new HashMap<String, HttpParam>();
    private Map<String, HttpParam> formParameters = new HashMap<String, HttpParam>();
    private List<BodyPart> multipartParams = new ArrayList<BodyPart>();
    private HeaderMap headers = new HeaderMap();

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
                           List<String> path,
                           Method method,
                           Map<String, HttpParam> queryParameters,
                           Map<String, HttpParam> formParameters,
                           List<BodyPart> multipartParams,
                           HeaderMap headers) {

        this.client = client;
        this.path = path;
        this.method = method;
        this.queryParameters = queryParameters;
        this.formParameters = formParameters;
        this.multipartParams = multipartParams;
        this.headers = headers;
    }

    public Client getClient() {
        return client;
    }

    public Map<String, HttpParam> getFormParameters() {
        return formParameters;
    }

    public HeaderMap getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        this.headers.addHeader(key, value);
    }

    public Method getMethod() {
        return method;
    }

    public List<BodyPart> getMultipartParams() {
        return multipartParams;
    }

    public Map<String, HttpParam> getQueryParameters() {
        return queryParameters;
    }

    public List<String> getPath() {
        return path;
    }

    public void setChildRequestBuilders(List<RequestBuilder> childRequestBuilders) {
        this.childRequestBuilders = childRequestBuilders;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setFormParameters(Map<String, HttpParam> formParameters) {
        this.formParameters = formParameters;
    }

    public void setHeaders(HeaderMap headers) {
        this.headers = headers;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setMultipartParams(List<BodyPart> multipartParams) {
        this.multipartParams = multipartParams;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public void setQueryParameters(Map<String, HttpParam> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getRelativePath() {
        return ListUtils.mkString(path, "/");
    }

    public void appendPathElement(String pathElement) {
        this.path.add(pathElement);
    }

    public void prependPathElements(List<String> pathElements) {
        this.path.addAll(0, pathElements);
    }

    public RequestBuilder cloneAddHeader(String key, String value) {
        RequestBuilder clone = this.shallowClone();
        clone.headers = this.headers.cloned();
        clone.addHeader(key, value);
        return clone;
    }

    public <B> Future<Response<String>> callToStringResponse(B body) {
        return client.callToStringResponse(this, body);
    }

    public <B, R> Future<Response<R>> callToTypeResponse(B body, String canonicalResponseType) {
        return client.callToTypeResponse(this, body, canonicalResponseType);
    }

    public RequestBuilder shallowClone() {
        RequestBuilder rb =
                new RequestBuilder(
                        this.client,
                        this.path,
                        this.method,
                        this.queryParameters,
                        this.formParameters,
                        this.multipartParams,
                        this.headers
                );
        rb.childRequestBuilders = new ArrayList<RequestBuilder>(this.childRequestBuilders);
        return rb;
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
