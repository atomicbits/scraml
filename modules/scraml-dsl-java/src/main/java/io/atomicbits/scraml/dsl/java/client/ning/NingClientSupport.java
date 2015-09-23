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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.multipart.*;
import io.atomicbits.scraml.dsl.java.*;
import io.atomicbits.scraml.dsl.java.ByteArrayPart;
import io.atomicbits.scraml.dsl.java.FilePart;
import io.atomicbits.scraml.dsl.java.StringPart;
import io.atomicbits.scraml.dsl.java.client.ClientConfig;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by peter on 20/09/15.
 * Copyright Atomic BITS b.v.b.a.
 */
public class NingClientSupport implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private ClientConfig config;
    private Map<String, String> defaultHeaders;

    private AsyncHttpClient ningClient;


    public NingClientSupport(String host,
                             int port,
                             String protocol,
                             String prefix,
                             ClientConfig config,
                             Map<String, String> defaultHeaders) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.prefix = prefix;
        this.config = config;
        this.defaultHeaders = defaultHeaders;

        AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
        this.ningClient = new AsyncHttpClient(applyConfiguration(configBuilder).build());
    }

    public ClientConfig getConfig() {
        return config;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCleanPrefix() {
        if (prefix != null) {
            String cleanPrefix = prefix;
            if (prefix.startsWith("/")) {
                cleanPrefix = cleanPrefix.substring(1);
            }
            if (prefix.endsWith("/")) {
                cleanPrefix = cleanPrefix.substring(0, cleanPrefix.length() - 1);
            }
            return "/" + cleanPrefix;
        } else {
            return "";
        }
    }

    @Override
    public Map<String, String> defaultHeaders() {
        return defaultHeaders;
    }

    private AsyncHttpClient getClient() {
        return ningClient;
    }

    private AsyncHttpClientConfig.Builder applyConfiguration(AsyncHttpClientConfig.Builder builder) {
        builder.setReadTimeout(config.getRequestTimeout());
        builder.setMaxConnections(config.getMaxConnections());
        builder.setRequestTimeout(config.getRequestTimeout());
        builder.setMaxRequestRetry(config.getMaxRequestRetry());
        builder.setConnectTimeout(config.getConnectTimeout());
        builder.setConnectionTTL(config.getConnectionTTL());
        builder.setWebSocketTimeout(config.getWebSocketTimeout());
        builder.setMaxConnectionsPerHost(config.getMaxConnectionsPerHost());
        builder.setAllowPoolingConnections(config.getAllowPoolingConnections());
        builder.setAllowPoolingSslConnections(config.getAllowPoolingSslConnections());
        builder.setPooledConnectionIdleTimeout(config.getPooledConnectionIdleTimeout());
        builder.setAcceptAnyCertificate(config.getAcceptAnyCertificate());
        builder.setFollowRedirect(config.getFollowRedirect());
        builder.setMaxRedirects(config.getMaxRedirects());
        builder.setRemoveQueryParamsOnRedirect(config.getRemoveQueryParamOnRedirect());
        builder.setStrict302Handling(config.getStrict302Handling());
        return builder;
    }

    @Override
    public <B> Future<Response<String>> callToStringResponse(RequestBuilder requestBuilder, B body) {
        // Create builder
        com.ning.http.client.RequestBuilder ningRb = new com.ning.http.client.RequestBuilder();
        String baseUrl = protocol + "://" + host + ":" + port + getCleanPrefix();
        ningRb.setUrl(baseUrl + "/" + requestBuilder.getRelativePath());
        ningRb.setMethod(requestBuilder.getMethod().name());


        Map<String, String> requestHeaders = new HashMap<String, String>(defaultHeaders);
        requestHeaders.putAll(requestBuilder.getHeaders());
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
             ningRb.addHeader(header.getKey(), header.getValue());
        }

        for (Map.Entry<String, HttpParam> queryParam : requestBuilder.getQueryParameters().entrySet()) {
            if (queryParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) queryParam.getValue();
                ningRb.addQueryParam(queryParam.getKey(), param.getParameter());
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) queryParam.getValue();
                for (String param : params.getParameters()) {
                    ningRb.addQueryParam(queryParam.getKey(), param);
                }
            }
        }

        if (body != null) {
            ObjectMapper mapper = new ObjectMapper();
            StringWriter writer = new StringWriter();
            try {
                mapper.writeValue(writer, body);
            } catch (IOException e) {
                throw new RuntimeException("JSON serialization of a " + body.getClass().getSimpleName() + " instance failed: " + body, e);
            }
            writer.flush();
            ningRb.setBody(writer.toString());
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignored, shouldn't happen on a StringWriter
            }
        }

        for (Map.Entry<String, HttpParam> formParam : requestBuilder.getFormParameters().entrySet()) {
            if (formParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) formParam.getValue();
                ningRb.addFormParam(formParam.getKey(), param.getParameter());
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) formParam.getValue();
                for (String param : params.getParameters()) {
                    ningRb.addFormParam(formParam.getKey(), param);
                }
            }
        }

        for (BodyPart bodyPart : requestBuilder.getMultipartParams()) {

            if (bodyPart.isString()) {
                StringPart part = (StringPart) bodyPart;
                ningRb.addBodyPart(
                        new com.ning.http.client.multipart.StringPart(
                                part.getName(),
                                part.getValue(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        )
                );
            }

            if (bodyPart.isFile()) {
                FilePart part = (FilePart) bodyPart;
                ningRb.addBodyPart(
                        new com.ning.http.client.multipart.FilePart(
                                part.getName(),
                                part.getFile(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getFileName(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        )
                );
            }

            if(bodyPart.isByteArray()) {
                ByteArrayPart part = (ByteArrayPart) bodyPart;
                ningRb.addBodyPart(
                        new com.ning.http.client.multipart.ByteArrayPart(
                                part.getName(),
                                part.getBytes(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getFileName(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        )
                );
            }

        }

        Request ningRequest = ningRb.build();
        // CompletableFuture is present in the JDS since 1.8
        final CompletableFuture<Response<String>> future = new CompletableFuture<Response<String>>();

        getClient().executeRequest(ningRequest, new AsyncCompletionHandler<String>() {

            @Override
            public String onCompleted(com.ning.http.client.Response response) throws Exception {
                try {
                    Response<String> resp =
                            new Response<String>(
                                    response.getResponseBody(config.getResponseCharset().displayName()),
                                    response.getResponseBody(config.getResponseCharset().displayName()),
                                    response.getStatusCode(),
                                    response.getHeaders()
                            );
                    future.complete(resp);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                super.onThrowable(t);
                future.completeExceptionally(t);
            }

        });

        return future;
    }


    @Override
    public <B, R> Future<Response<R>> callToTypeResponse(RequestBuilder request, B body) {
        return null;
    }

    @Override
    public void close() {
        if (ningClient != null) {
            ningClient.close();
        }
    }

}
