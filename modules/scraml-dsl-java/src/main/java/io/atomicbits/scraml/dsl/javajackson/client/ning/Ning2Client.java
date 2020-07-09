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

import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;

import io.atomicbits.scraml.dsl.javajackson.*;
import io.atomicbits.scraml.dsl.javajackson.client.ClientConfig;
import io.atomicbits.scraml.dsl.javajackson.json.Json;
import io.atomicbits.scraml.dsl.javajackson.ByteArrayPart;
import io.atomicbits.scraml.dsl.javajackson.FilePart;
import io.atomicbits.scraml.dsl.javajackson.StringPart;
import org.asynchttpclient.request.body.generator.InputStreamBodyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.asynchttpclient.Dsl.*;


/**
 * Created by peter on 08/07/2020.
 */
public class Ning2Client implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private ClientConfig config;
    private Map<String, String> defaultHeaders;

    private AsyncHttpClient ningClient;

    private Logger LOGGER = LoggerFactory.getLogger(Ning2Client.class);

    public Ning2Client(String host,
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

        DefaultAsyncHttpClientConfig.Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder();
        this.ningClient = asyncHttpClient(applyConfiguration(configBuilder).build());
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

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
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


    private AsyncHttpClient getClient() {
        return ningClient;
    }

    private DefaultAsyncHttpClientConfig.Builder applyConfiguration(DefaultAsyncHttpClientConfig.Builder builder) {
        builder.setReadTimeout(config.getRequestTimeout());
        builder.setMaxConnections(config.getMaxConnections());
        builder.setRequestTimeout(config.getRequestTimeout());
        builder.setMaxRequestRetry(config.getMaxRequestRetry());
        builder.setConnectTimeout(config.getConnectTimeout());
        builder.setConnectionTtl(config.getConnectionTTL());
        // builder.setWebSocketTimeout(config.getWebSocketTimeout());
        builder.setMaxConnectionsPerHost(config.getMaxConnectionsPerHost());
        // builder.setAllowPoolingConnections(config.getAllowPoolingConnections());
        // builder.setAllowPoolingSslConnections(config.getAllowPoolingSslConnections());
        builder.setPooledConnectionIdleTimeout(config.getPooledConnectionIdleTimeout());
        builder.setUseInsecureTrustManager(config.getUseInsecureTrustManager());
        builder.setFollowRedirect(config.getFollowRedirect());
        builder.setMaxRedirects(config.getMaxRedirects());
        builder.setStrict302Handling(config.getStrict302Handling());
        builder.setSslContext(config.getSslContext());
        builder.setSslSessionCacheSize(config.getSslSessionCacheSize());
        builder.setSslSessionTimeout(config.getSslSessionTimeout());
        // builder.setHostnameVerifier(config.getHostnameVerifier());
        return builder;
    }


    @Override
    public CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<String>> callToStringResponse(io.atomicbits.scraml.dsl.javajackson.RequestBuilder requestBuilder,
                                                                                                         String body) {
        return callToResponse(requestBuilder, body, this::transformToStringBody);
    }


    @Override
    public CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<BinaryData>> callToBinaryResponse(io.atomicbits.scraml.dsl.javajackson.RequestBuilder requestBuilder,
                                                                                                             String body) {
        return callToResponse(requestBuilder, body, this::transformToBinaryBody);
    }


    @Override
    public <R> CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<R>> callToTypeResponse(io.atomicbits.scraml.dsl.javajackson.RequestBuilder requestBuilder,
                                                                                                      String body,
                                                                                                      String canonicalResponseType) {
        return callToResponse(requestBuilder, body, (result) -> transformToTypedBody(result, canonicalResponseType));
    }


    private <R> CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<R>> callToResponse(io.atomicbits.scraml.dsl.javajackson.RequestBuilder requestBuilder,
                                                                                                   String body,
                                                                                                   Function<org.asynchttpclient.Response, io.atomicbits.scraml.dsl.javajackson.Response<R>> transformer) {
        // Create builder
        org.asynchttpclient.RequestBuilder ningRb = new org.asynchttpclient.RequestBuilder();
        String baseUrl = protocol + "://" + host + ":" + port + getCleanPrefix();
        ningRb.setUrl(baseUrl + "/" + requestBuilder.getRelativePath());
        ningRb.setMethod(requestBuilder.getMethod().name());


        HeaderMap requestHeaders = new HeaderMap();
        requestHeaders.setHeaders(defaultHeaders);
        requestHeaders.setHeaders(requestBuilder.getHeaderMap());
        for (Map.Entry<String, List<String>> header : requestHeaders.getHeaders().entrySet()) {
            for (String value : header.getValue()) {
                ningRb.addHeader(header.getKey(), value);
            }
        }

        for (Map.Entry<String, HttpParam> queryParam : requestBuilder.getQueryParameters().entrySet()) {
            if (queryParam.getValue() instanceof RepeatedHttpParam) {
                RepeatedHttpParam params = (RepeatedHttpParam) queryParam.getValue();
                if (params.getParameters() != null) {
                    for (String param : params.getParameters()) {
                        ningRb.addQueryParam(queryParam.getKey(), param);
                    }
                }
            } else if (queryParam.getValue() instanceof SingleHttpParam) {
                SingleHttpParam param = (SingleHttpParam) queryParam.getValue();
                if (param.getParameter() != null) {
                    ningRb.addQueryParam(queryParam.getKey(), param.getParameter());
                }
            }
        }

        if (body != null) {
            ningRb.setBody(body);
        }

        if (requestBuilder.getBinaryRequest() != null) {
            BinaryRequest binaryRequest = requestBuilder.getBinaryRequest();
            if (binaryRequest.isFile()) {
                File file = ((FileBinaryRequest) binaryRequest).getFile();
                ningRb.setBody(file);
            }
            if (binaryRequest.isInputStream()) {
                InputStream stream = ((InputStreamBinaryRequest) binaryRequest).getInputStream();
                ningRb.setBody(new InputStreamBodyGenerator(stream));
            }
            if (binaryRequest.isByteArray()) {
                byte[] bytes = ((ByteArrayBinaryRequest) binaryRequest).getBytes();
                ningRb.setBody(bytes);
            }
            if (binaryRequest.isString()) {
                String text = ((StringBinaryRequest) binaryRequest).getText();
                ningRb.setBody(text);
            }
        }

        for (Map.Entry<String, HttpParam> formParam : requestBuilder.getFormParameters().entrySet()) {
            if (formParam.getValue() instanceof RepeatedHttpParam) {
                RepeatedHttpParam params = (RepeatedHttpParam) formParam.getValue();
                if (params.getParameters() != null) {
                    for (String param : params.getParameters()) {
                        ningRb.addFormParam(formParam.getKey(), param);
                    }
                }
            } else if (formParam.getValue() instanceof SingleHttpParam) {
                SingleHttpParam param = (SingleHttpParam) formParam.getValue();
                if (param.getParameter() != null) {
                    ningRb.addFormParam(formParam.getKey(), param.getParameter());
                }
            }
        }

        for (BodyPart bodyPart : requestBuilder.getMultipartParams()) {

            if (bodyPart.isString()) {
                StringPart part = (StringPart) bodyPart;
                ningRb.addBodyPart(
                        new org.asynchttpclient.request.body.multipart.StringPart(
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
                        new org.asynchttpclient.request.body.multipart.FilePart(
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
                        new org.asynchttpclient.request.body.multipart.ByteArrayPart(
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
        final CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<R>> future = new CompletableFuture<io.atomicbits.scraml.dsl.javajackson.Response<R>>();

        LOGGER.debug("Executing request: " + ningRequest + "\nWith 'string' body: " + ningRequest.getStringData());

        getClient().executeRequest(ningRequest, new AsyncCompletionHandler<String>() {

            private org.asynchttpclient.Response response;

            @Override
            public String onCompleted(org.asynchttpclient.Response response) throws Exception {
                this.response = response;
                try {
                    io.atomicbits.scraml.dsl.javajackson.Response<R> resp = transformer.apply(this.response);
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


    private io.atomicbits.scraml.dsl.javajackson.Response<String> transformToStringBody(org.asynchttpclient.Response response) {
        Map<String, List<String>> headers = headersToMap(response.getHeaders());
        String responseBody =
                response.getResponseBody(
                        getResponseCharsetFromHeaders(headers, config.getResponseCharset())
                );
        return new io.atomicbits.scraml.dsl.javajackson.Response<String>(
                responseBody,
                responseBody,
                response.getStatusCode(),
                headers
        );
    }


    private io.atomicbits.scraml.dsl.javajackson.Response<BinaryData> transformToBinaryBody(org.asynchttpclient.Response response) {
        Map<String, List<String>> headers = headersToMap(response.getHeaders());
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
            // there are many responses in the 200 range with different typed responses.
            BinaryData binaryData = new Ning2BinaryData(response);
            return new io.atomicbits.scraml.dsl.javajackson.Response<BinaryData>(
                    null,
                    binaryData,
                    response.getStatusCode(),
                    headers
            );
        } else {
            String responseBody =
                    response.getResponseBody(
                            getResponseCharsetFromHeaders(headers, config.getResponseCharset())
                    );
            return new io.atomicbits.scraml.dsl.javajackson.Response<BinaryData>(
                    responseBody,
                    null,
                    response.getStatusCode(),
                    headers
            );
        }
    }


    private <R> io.atomicbits.scraml.dsl.javajackson.Response<R> transformToTypedBody(org.asynchttpclient.Response response, String canonicalResponseType) {
        Map<String, List<String>> headers = headersToMap(response.getHeaders());
        String responseBody =
                response.getResponseBody(
                        getResponseCharsetFromHeaders(headers, config.getResponseCharset())
                );
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
            // there are many responses in the 200 range with different typed responses.
            return new io.atomicbits.scraml.dsl.javajackson.Response<R>(
                    responseBody,
                    Json.parseBodyToObject(responseBody, canonicalResponseType),
                    response.getStatusCode(),
                    headers
            );
        } else {
            return new io.atomicbits.scraml.dsl.javajackson.Response<R>(
                    responseBody,
                    null,
                    response.getStatusCode(),
                    headers
            );
        }
    }


    @Override
    public void close() throws Exception {
        if (ningClient != null) {
            ningClient.close();
        }
    }

    Charset getResponseCharsetFromHeaders(Map<String, List<String>> headers, Charset defaultCharset) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("content-type".equals(entry.getKey().toLowerCase())) {
                for (String value : entry.getValue()) {
                    String[] parts = value.toLowerCase().split(";");
                    for (String part : parts) {
                        if (part.contains("charset")) {
                            String[] charsetSplit = value.toLowerCase().split("charset");
                            if (charsetSplit.length > 1) {
                                String charsetValue = charsetSplit[1];
                                String cleanValue = charsetValue.replace('=', ' ').trim();
                                try {
                                    return Charset.forName(cleanValue);
                                } catch (Throwable e) {
                                    // ignore, we'll fallback to the default charset
                                }
                            }
                        }
                    }
                }
            }
        }
        return defaultCharset;
    }

    Map<String, List<String>> headersToMap(HttpHeaders httpHeaders) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String name : httpHeaders.names()) {
            map.put(name, httpHeaders.getAll(name));
        }
        return map;
    }

}
