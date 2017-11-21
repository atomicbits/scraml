/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson.client.okhttp;

import io.atomicbits.scraml.dsl.androidjavajackson.*;
import io.atomicbits.scraml.dsl.androidjavajackson.Callback;
import io.atomicbits.scraml.dsl.androidjavajackson.client.ClientConfig;
import okhttp3.*;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 3/11/17.
 */
public class OkHttpScramlClient implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private ClientConfig config;
    private Map<String, String> defaultHeaders;

    private OkHttpClient okHttpClient;

    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public OkHttpScramlClient(String host,
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


        // Configure the client
        // See: https://github.com/square/okhttp/wiki/Recipes

        int maxIdleConnectionsPerHost = config.getMaxIdleConnectionsPerHost();

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        long keepAliveDuration;
        if (config.getConnectionTTL() == -1) {
            keepAliveDuration = 5 * 60 * 1000;
        } else {
            keepAliveDuration = config.getConnectionTTL();
        }

        ConnectionPool connectionPool = new ConnectionPool(maxIdleConnectionsPerHost, keepAliveDuration, timeUnit);

        // We don't need to be able to set all configuration options, but we list them all below for reference
        this.okHttpClient =
                new OkHttpClient.Builder()
                        // .protocols(null) // always use the default {Protocol.HTTP_2, Protocol.HTTP_1_1}
                        .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                        .writeTimeout(config.getWriteTimeout(), TimeUnit.MILLISECONDS)
                        // .pingInterval(0, TimeUnit.MILLISECONDS) // interval between web socket pings, default is 0 => disable client-initiated pings
                        .connectionPool(connectionPool)
                        // .connectionSpecs(null) // use the defaults, see: https://github.com/square/okhttp/wiki/HTTPS
                        // .cache(null) // we probably don't need response caching on a REST client
                        .followRedirects(config.getFollowRedirect())
                        .followSslRedirects(config.getFollowRedirect())
                        .retryOnConnectionFailure(config.getMaxRequestRetry() > 0)
                        // .proxy(null) // set the proxy
                        // .proxyAuthenticator(null)
                        // .socketFactory(null) // use the default
                        .sslSocketFactory(config.getSslContext().getSocketFactory(), config.getTrustManager()) // use for client certificates
                        // .hostnameVerifier(null)
                        // .authenticator(null) // use for basic auth
                        // .certificatePinner(null) // see: https://github.com/square/okhttp/wiki/HTTPS
                        // .cookieJar(null) // Default is no automatic cookie handling
                        // .dispatcher(null)
                        // .dns(null) // use the system DNS by default
                        // .eventListener(null) // useful for collecting metrics or logging requests
                        // .eventListenerFactory(null) // useful for creating per-call scoped listeners
                        // .addInterceptor(null) // usefull for intercepting or changing the request chain
                        // .addNetworkInterceptor(null) // similar to 'addInterceptor', don't see the difference at this point
                        .build();

    }


    @Override
    public void callToStringResponse(RequestBuilder requestBuilder, String body, Callback<String> callback) {

    }

    @Override
    public void callToBinaryResponse(RequestBuilder requestBuilder, String body, Callback<BinaryData> callback) {

    }

    @Override
    public <R> void callToTypeResponse(RequestBuilder requestBuilder,
                                       String body,
                                       String canonicalResponseType,
                                       final Callback<R> callback) {

    }


    public <R> void callToResponse(RequestBuilder requestBuilder,
                                   String body,
                                   final Callback<R> callback) {

        Headers.Builder headerBuilder = new Headers.Builder();

        for (Map.Entry<String, String> entrySet : defaultHeaders.entrySet()) {
            headerBuilder = headerBuilder.add(entrySet.getKey(), entrySet.getValue());
        }

        for (Map.Entry<String, List<String>> entrySet : requestBuilder.getHeaderMap().getHeaders().entrySet()) {
            if (!entrySet.getValue().isEmpty()) {
                headerBuilder = headerBuilder.add(entrySet.getKey(), entrySet.getValue().get(0));
            }
        }

        Headers headers = headerBuilder.build();


        HttpUrl.Builder urlBuilder =
                new HttpUrl.Builder()
                        .scheme(protocol)
                        .host(host)
                        .port(port)
                        .addPathSegments(getCleanPrefix())
                        .addPathSegments(requestBuilder.getRelativePath());

        for (Map.Entry<String, HttpParam> queryParam : requestBuilder.getQueryParameters().entrySet()) {
            if (queryParam.getValue() instanceof RepeatedHttpParam) {
                RepeatedHttpParam params = (RepeatedHttpParam) queryParam.getValue();
                if (params.getParameters() != null) {
                    for (String param : params.getParameters()) {
                        urlBuilder = urlBuilder.addQueryParameter(queryParam.getKey(), param);
                    }
                }
            } else if (queryParam.getValue() instanceof SingleHttpParam) {
                SingleHttpParam param = (SingleHttpParam) queryParam.getValue();
                if (param.getParameter() != null) {
                    urlBuilder = urlBuilder.addQueryParameter(queryParam.getKey(), param.getParameter());
                }
            }
        }


        HttpUrl url = urlBuilder.build();


        RequestBody requestBody = null;

        String contentType = headers.get("Content-Type");
        MediaType mediaType = null;
        if (contentType != null) {
            mediaType = MediaType.parse(contentType);
        }

        if (body != null) {
            requestBody = RequestBody.create(mediaType, body);
        }

        if (requestBuilder.getBinaryRequest() != null) {
            BinaryRequest binaryRequest = requestBuilder.getBinaryRequest();
            if (binaryRequest.isFile()) {
                File file = ((FileBinaryRequest) binaryRequest).getFile();
                requestBody = RequestBody.create(mediaType, file);
            }
            if (binaryRequest.isInputStream()) {
                try {
                    InputStream stream = ((InputStreamBinaryRequest) binaryRequest).getInputStream();
                    int[] lengthWrapper = new int[1];
                    byte[] cachedBytes = new byte[0];
                    cachedBytes = readFully(stream, lengthWrapper);
                    int cachedBytesLenght = lengthWrapper[0];
                    requestBody = RequestBody.create(mediaType, cachedBytes, 0, cachedBytesLenght);
                } catch (IOException e) {
                    callback.onFailure(e);
                }
            }
            if (binaryRequest.isByteArray()) {
                byte[] bytes = ((ByteArrayBinaryRequest) binaryRequest).getBytes();
                requestBody = RequestBody.create(mediaType, bytes);
            }
            if (binaryRequest.isString()) {
                String text = ((StringBinaryRequest) binaryRequest).getText();
                requestBody = RequestBody.create(mediaType, text);
            }
        }

        if (!requestBuilder.getFormParameters().isEmpty()) {
            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (Map.Entry<String, HttpParam> formParam : requestBuilder.getFormParameters().entrySet()) {
                if (formParam.getValue() instanceof RepeatedHttpParam) {
                    RepeatedHttpParam params = (RepeatedHttpParam) formParam.getValue();
                    if (params.getParameters() != null) {
                        for (String param : params.getParameters()) {
                            formBodyBuilder.add(formParam.getKey(), param);
                        }
                    }
                } else if (formParam.getValue() instanceof SingleHttpParam) {
                    SingleHttpParam param = (SingleHttpParam) formParam.getValue();
                    if (param.getParameter() != null) {
                        formBodyBuilder.add(formParam.getKey(), param.getParameter());
                    }
                }
            }
            requestBody = formBodyBuilder.build();
        }


        if (!requestBuilder.getMultipartParams().isEmpty()) {

            MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder();

            for (BodyPart bodyPart : requestBuilder.getMultipartParams()) {

                if (bodyPart.isString()) {
                    StringPart part = (StringPart) bodyPart;
                    multipartBodyBuilder.addFormDataPart(part.getName(), part.getValue());
                }

                if (bodyPart.isFile()) {
                    FilePart part = (FilePart) bodyPart;
                    MediaType contentMediaType = MediaType.parse(part.getContentType());
                    multipartBodyBuilder
                            .addFormDataPart(
                                    part.getName(),
                                    part.getFileName(),
                                    RequestBody.create(contentMediaType, part.getFile())
                            );
                }

                if (bodyPart.isByteArray()) {
                    ByteArrayPart part = (ByteArrayPart) bodyPart;
                    MediaType contentMediaType = MediaType.parse(part.getContentType());
                    multipartBodyBuilder
                            .addFormDataPart(
                                    part.getName(),
                                    part.getFileName(),
                                    RequestBody.create(contentMediaType, part.getBytes())
                            );
                }

            }

            requestBody = multipartBodyBuilder.build();
        }


        Request request =
                new Request.Builder()
                        .headers(headers)
                        .method(requestBuilder.getMethod().name(), requestBody)
                        .url(url)
                        .build();


        getClient().newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                try {
                    if (response.isSuccessful()) {
                        io.atomicbits.scraml.dsl.androidjavajackson.Response<R> scramlResponse =
                                null;
                        callback.onOkResponse(scramlResponse);
                    } else {

                        // callback.onNokResponse();
                    }

                    //  Headers responseHeaders = response.headers();
                    //  for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    //      System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    //  }
                    //  System.out.println(responseBody.string());
                } catch (Throwable t) {
                    callback.onFailure(t);
                }
            }

        });

    }

    private io.atomicbits.scraml.dsl.androidjavajackson.Response<String> transformToStringBody(Response response) throws IOException {

        ResponseBody responseBody = response.body();
        String responseString;
        if (responseBody != null) {
            responseString = responseBody.string();
        } else {
            responseString = "";
        }

        return new io.atomicbits.scraml.dsl.androidjavajackson.Response<String>(
                responseString,
                responseString,
                response.code(),
                response.headers().toMultimap()
        );
    }

    /**
     * NOTICE This part reuses software under the Apache 2.0 license.
     * See com.ning.http.util.AsyncHttpProviderUtils in async-http-client
     */
    private byte[] readFully(InputStream in, int[] lengthWrapper) throws IOException {
        // just in case available() returns bogus (or -1), allocate non-trivial chunk
        byte[] b = new byte[Math.max(512, in.available())];
        int offset = 0;
        while (true) {
            int left = b.length - offset;
            int count = in.read(b, offset, left);
            if (count < 0) { // EOF
                break;
            }
            offset += count;
            if (count == left) { // full buffer, need to expand
                b = doubleUp(b);
            }
        }
        // wish Java had Tuple return type...
        lengthWrapper[0] = offset;
        return b;
    }

    /**
     * NOTICE This part reuses software under the Apache 2.0 license.
     * See com.ning.http.util.AsyncHttpProviderUtils in async-http-client
     */
    private byte[] doubleUp(byte[] b) {
        int len = b.length;
        byte[] b2 = new byte[len + len];
        System.arraycopy(b, 0, b2, 0, len);
        return b2;
    }


    @Override
    public ClientConfig getConfig() {
        return this.config;
    }

    @Override
    public Map<String, String> getDefaultHeaders() {
        return this.defaultHeaders;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
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
    public void close() {
    }

    public OkHttpClient getClient() {
        return okHttpClient;
    }

}
