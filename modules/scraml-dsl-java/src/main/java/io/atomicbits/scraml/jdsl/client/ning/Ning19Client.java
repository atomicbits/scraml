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

package io.atomicbits.scraml.jdsl.client.ning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ning.http.client.*;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import io.atomicbits.scraml.jdsl.*;
import io.atomicbits.scraml.jdsl.client.ClientConfig;
import io.atomicbits.scraml.jdsl.ByteArrayPart;
import io.atomicbits.scraml.jdsl.FilePart;
import io.atomicbits.scraml.jdsl.StringPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Ning19Client implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private ClientConfig config;
    private Map<String, String> defaultHeaders;

    private AsyncHttpClient ningClient;

    private Logger LOGGER = LoggerFactory.getLogger(Ning19Client.class);

    /**
     * Reuse of ObjectMapper and JsonFactory is very easy: they are thread-safe provided that configuration is done before any use
     * (and from a single thread). After initial configuration use is fully thread-safe and does not need to be explicitly synchronized.
     * Source: http://wiki.fasterxml.com/JacksonBestPracticesPerformance
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    private ArrayList<String> javaPrimitiveTypes = new ArrayList<String>() {{
        add("java.lang.String");
        add("java.lang.Boolean");
        add("java.lang.Byte");
        add("java.lang.Character");
        add("java.lang.Double");
        add("java.lang.Float");
        add("java.lang.Integer");
        add("java.lang.Number");
        add("java.lang.Long");
        add("java.lang.Short");
    }};


    public Ning19Client(String host,
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
    public <B> CompletableFuture<io.atomicbits.scraml.jdsl.Response<String>> callToStringResponse(io.atomicbits.scraml.jdsl.RequestBuilder requestBuilder,
                                                                                                  B body,
                                                                                                  String canonicalContentType) {
        return callToResponse(requestBuilder, body, canonicalContentType, this::transformToStringBody);
    }


    @Override
    public <B> CompletableFuture<io.atomicbits.scraml.jdsl.Response<BinaryData>> callToBinaryResponse(io.atomicbits.scraml.jdsl.RequestBuilder requestBuilder,
                                                                                                      B body,
                                                                                                      String canonicalContentType) {
        return callToResponse(requestBuilder, body, canonicalContentType, this::transformToBinaryBody);
    }


    @Override
    public <B, R> CompletableFuture<io.atomicbits.scraml.jdsl.Response<R>> callToTypeResponse(io.atomicbits.scraml.jdsl.RequestBuilder requestBuilder,
                                                                                              B body,
                                                                                              String canonicalContentType,
                                                                                              String canonicalResponseType) {
        return callToResponse(requestBuilder, body, canonicalContentType, (result) -> transformToTypedBody(result, canonicalResponseType));
    }


    protected <B, R> CompletableFuture<io.atomicbits.scraml.jdsl.Response<R>> callToResponse(io.atomicbits.scraml.jdsl.RequestBuilder requestBuilder,
                                                                                             B body,
                                                                                             String canonicalContentType,
                                                                                             Function<com.ning.http.client.Response, io.atomicbits.scraml.jdsl.Response<R>> transformer) {
        // Create builder
        com.ning.http.client.RequestBuilder ningRb = new com.ning.http.client.RequestBuilder();
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
        final CompletableFuture<io.atomicbits.scraml.jdsl.Response<R>> future = new CompletableFuture<io.atomicbits.scraml.jdsl.Response<R>>();

        LOGGER.debug("Executing request: " + ningRequest + "\nWith 'string' body: " + ningRequest.getStringData());

        getClient().executeRequest(ningRequest, new AsyncCompletionHandler<String>() {

            @Override
            public String onCompleted(com.ning.http.client.Response response) throws Exception {
                try {
                    io.atomicbits.scraml.jdsl.Response<R> resp = transformer.apply(response);
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


    private io.atomicbits.scraml.jdsl.Response<String> transformToStringBody(com.ning.http.client.Response response) {
        try {
            String responseBody =
                    response.getResponseBody(
                            getResponseCharsetFromHeaders(response.getHeaders(), config.getResponseCharset().displayName())
                    );
            return new io.atomicbits.scraml.jdsl.Response<String>(
                    responseBody,
                    responseBody,
                    response.getStatusCode(),
                    response.getHeaders()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private io.atomicbits.scraml.jdsl.Response<BinaryData> transformToBinaryBody(com.ning.http.client.Response response) {
        try {
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
                // there are many responses in the 200 range with different typed responses.
                BinaryData binaryData = new Ning19BinaryData(response);
                return new io.atomicbits.scraml.jdsl.Response<BinaryData>(
                        null,
                        binaryData,
                        response.getStatusCode(),
                        response.getHeaders()
                );
            } else {
                String responseBody =
                        response.getResponseBody(
                                getResponseCharsetFromHeaders(response.getHeaders(), config.getResponseCharset().displayName())
                        );
                return new io.atomicbits.scraml.jdsl.Response<BinaryData>(
                        responseBody,
                        null,
                        response.getStatusCode(),
                        response.getHeaders()
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private <R> io.atomicbits.scraml.jdsl.Response<R> transformToTypedBody(com.ning.http.client.Response response, String canonicalResponseType) {
        try {
            String responseBody =
                    response.getResponseBody(
                            getResponseCharsetFromHeaders(response.getHeaders(), config.getResponseCharset().displayName())
                    );
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
                // there are many responses in the 200 range with different typed responses.
                return new io.atomicbits.scraml.jdsl.Response<R>(
                        responseBody,
                        parseBodyToObject(responseBody, canonicalResponseType),
                        response.getStatusCode(),
                        response.getHeaders()
                );
            } else {
                return new io.atomicbits.scraml.jdsl.Response<R>(
                        responseBody,
                        null,
                        response.getStatusCode(),
                        response.getHeaders()
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        if (canonicalRequestType != null && !isPrimitiveType(canonicalRequestType)) {
            JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalRequestType);
            ObjectWriter writer = this.objectMapper.writerFor(javaType);
            try {
                return writer.writeValueAsString(body);
            } catch (IOException e) {
                throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
            }
        } else {
            return body.toString();
        }
    }

    private boolean isPrimitiveType(String type) {
        return javaPrimitiveTypes.contains(type);
    }

    private <R> R parseBodyToObject(String body, String canonicalResponseType) {
        JavaType javaType = TypeFactory.defaultInstance().constructFromCanonical(canonicalResponseType);
        try {
            return this.objectMapper.readValue(body, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON parse error: " + e.getMessage(), e);
        }
    }


    String getResponseCharsetFromHeaders(Map<String, List<String>> headers, String defaultCharset) {
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
                                    return Charset.forName(cleanValue).name();
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

}
