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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import io.atomicbits.scraml.dsl.java.*;
import io.atomicbits.scraml.dsl.java.ByteArrayPart;
import io.atomicbits.scraml.dsl.java.FilePart;
import io.atomicbits.scraml.dsl.java.StringPart;
import io.atomicbits.scraml.dsl.java.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

    private Logger LOGGER = LoggerFactory.getLogger(NingClientSupport.class);

    /**
     * Reuse of ObjectMapper and JsonFactory is very easy: they are thread-safe provided that configuration is done before any use
     * (and from a single thread). After initial configuration use is fully thread-safe and does not need to be explicitly synchronized.
     * Source: http://wiki.fasterxml.com/JacksonBestPracticesPerformance
     */
    private ObjectMapper objectMapper = new ObjectMapper();


    public NingClientSupport(String host,
                             Integer port,
                             String protocol,
                             String prefix,
                             ClientConfig config,
                             Map<String, String> defaultHeaders) {
        if (host != null) {
            this.host = host;
        } else {
            this.host = "localhost";
        }
        if (port != null) {
            this.port = port;
        } else {
            this.port = 80;
        }
        if (protocol != null) {
            this.protocol = protocol;
        } else {
            this.protocol = "http";
        }
        this.prefix = prefix;
        if (config != null) {
            this.config = config;
        } else {
            this.config = new ClientConfig();
        }
        if (defaultHeaders != null) {
            this.defaultHeaders = defaultHeaders;
        } else {
            this.defaultHeaders = new HashMap<>();
        }

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        builder.setStrict302Handling(config.getStrict302Handling());
        return builder;
    }


    @Override
    public <B> CompletableFuture<Response<String>> callToStringResponse(RequestBuilder requestBuilder, B body, String canonicalContentType) {
        return callToTransformedResponse(requestBuilder, body, canonicalContentType, (result) -> result);
    }


    @Override
    public <B, R> CompletableFuture<Response<R>> callToTypeResponse(RequestBuilder requestBuilder, B body, String canonicalContentType, String canonicalResponseType) {
        return callToTransformedResponse(requestBuilder, body, canonicalContentType, (result) -> parseBodyToObject(canonicalResponseType, result));
    }


    protected <B, R> CompletableFuture<Response<R>> callToTransformedResponse(RequestBuilder requestBuilder,
                                                                              B body,
                                                                              String canonicalContentType,
                                                                              Function<String, R> transformer) {
        // Create builder
        com.ning.http.client.RequestBuilder ningRb = new com.ning.http.client.RequestBuilder();
        String baseUrl = protocol + "://" + host + ":" + port + getCleanPrefix();
        ningRb.setUrl(baseUrl + "/" + requestBuilder.getRelativePath());
        ningRb.setMethod(requestBuilder.getMethod().name());


        HeaderMap requestHeaders = new HeaderMap();
        requestHeaders.addHeaders(defaultHeaders);
        requestHeaders.addHeaders(requestBuilder.getHeaders());
        for (Map.Entry<String, List<String>> header : requestHeaders.getHeaders().entrySet()) {
            for (String value : header.getValue()) {
                ningRb.addHeader(header.getKey(), value);
            }
        }

        for (Map.Entry<String, HttpParam> queryParam : requestBuilder.getQueryParameters().entrySet()) {
            if (queryParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) queryParam.getValue();
                if (param.getParameter() != null) {
                    ningRb.addQueryParam(queryParam.getKey(), param.getParameter());
                }
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) queryParam.getValue();
                if (params.getParameters() != null) {
                    for (String param : params.getParameters()) {
                        ningRb.addQueryParam(queryParam.getKey(), param);
                    }
                }
            }
        }

        if (body != null) {
            ningRb.setBody(writeBodyToString(canonicalContentType, body));
        }

        for (Map.Entry<String, HttpParam> formParam : requestBuilder.getFormParameters().entrySet()) {
            if (formParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) formParam.getValue();
                if (param.getParameter() != null) {
                    ningRb.addFormParam(formParam.getKey(), param.getParameter());
                }
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) formParam.getValue();
                if (params.getParameters() != null) {
                    for (String param : params.getParameters()) {
                        ningRb.addFormParam(formParam.getKey(), param);
                    }
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

            if (bodyPart.isByteArray()) {
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
        // CompletableFuture is present in the JDK since 1.8
        final CompletableFuture<Response<R>> future = new CompletableFuture<Response<R>>();

        LOGGER.debug("Executing request: " + ningRequest + "\nWith body: " + ningRequest.getStringData());

        getClient().executeRequest(ningRequest, new AsyncCompletionHandler<String>() {

            @Override
            public String onCompleted(com.ning.http.client.Response response) throws Exception {
                try {
                    String responseBody = response.getResponseBody(config.getResponseCharset().displayName());

                    Response<R> resp =
                            new Response<R>(
                                    responseBody,
                                    transformer.apply(responseBody),
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
    public void close() {
        if (ningClient != null) {
            ningClient.close();
        }
    }


    /**
     * Write the body to a JSON string.
     * <p>
     * The main reason why we need the canonical form of the request type to serialize the body is in cases where
     * Java type erasure hides access to the Json annotations of our transfer objects.
     * <p>
     * Examples of such type erasure are cases where types in a hierarchy are put inside a java.util.List<B>. Then, the type of
     * <B> is hidden in java.util.List<?>, which hides the @JsonTypeInfo annotations for the objectmapper so that all type info
     * disappears form the resulting JSON objects.
     *
     * @param canonicalRequestType The canonical form of the request body.
     * @param body                 The actual body.
     * @param <B>                  The type of the body.
     * @return The JSON representation of the body as a string.
     */
    private <B> String writeBodyToString(String canonicalRequestType, B body) {
        if (canonicalRequestType != null) {
            JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalRequestType);
            ObjectWriter writer = this.objectMapper.writerFor(javaType);
            try {
                return writer.writeValueAsString(body);
            } catch (IOException e) {
                throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
            }
        } else {
            try {
                return this.objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
            }
        }
    }


    private <R> R parseBodyToObject(String canonicalResponseType, String body) {
        JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalResponseType);
        try {
            return this.objectMapper.readValue(body, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }

}
