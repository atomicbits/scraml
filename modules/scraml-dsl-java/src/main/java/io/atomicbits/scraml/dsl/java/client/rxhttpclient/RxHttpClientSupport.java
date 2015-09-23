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

package io.atomicbits.scraml.dsl.java.client.rxhttpclient;

import be.wegenenverkeer.rxhttp.ClientRequest;
import be.wegenenverkeer.rxhttp.ClientRequestBuilder;
import be.wegenenverkeer.rxhttp.RxHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.atomicbits.scraml.dsl.java.*;
import io.atomicbits.scraml.dsl.java.client.ClientConfig;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;


public class RxHttpClientSupport implements Client {

    private String protocol;
    private String host;
    private int port;
    private String prefix;
    private ClientConfig config;
    private Map<String, String> defaultHeaders;

    private RxHttpClient client = null;

    public RxHttpClientSupport(String host,
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
    }


    public ClientConfig getConfig() {
        return config;
    }

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    public String getProtocol() {
        return protocol;
    }

    @Override
    public Map<String, String> defaultHeaders() {
        return defaultHeaders;
    }

    private RxHttpClient getClient() {
        if (this.client == null) {
            RxHttpClient.Builder builder = new RxHttpClient.Builder().setBaseUrl(protocol + "://" + host + ":" + port + getCleanPrefix());
            this.client = applyConfiguration(builder).build();
        }
        return this.client;
    }

    private RxHttpClient.Builder applyConfiguration(RxHttpClient.Builder builder) {
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


    private <B> Future<Response<String>> callTo200Response(RequestBuilder requestBuilder, B body) {

        ClientRequestBuilder clientWithResourcePathAndMethod =
                client.requestBuilder()
                        .setUrlRelativetoBase(requestBuilder.getRelativePath())
                        .setMethod(requestBuilder.getMethod().toString());

        Map<String, String> requestHeaders = new HashMap<String, String>(defaultHeaders);
        requestHeaders.putAll(requestBuilder.getHeaders());
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            clientWithResourcePathAndMethod.addHeader(header.getKey(), header.getValue());
        }

        for (Map.Entry<String, HttpParam> queryParam : requestBuilder.getQueryParameters().entrySet()) {
            if (queryParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) queryParam.getValue();
                clientWithResourcePathAndMethod.addQueryParam(queryParam.getKey(), param.getParameter());
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) queryParam.getValue();
                for (String param : params.getParameters()) {
                    clientWithResourcePathAndMethod.addQueryParam(queryParam.getKey(), param);
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
            clientWithResourcePathAndMethod.setBody(writer.toString());
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignored
            }
        }

        for (Map.Entry<String, HttpParam> formParam : requestBuilder.getFormParameters().entrySet()) {
            if (formParam.getValue().isSingle()) {
                SingleHttpParam param = (SingleHttpParam) formParam.getValue();
                clientWithResourcePathAndMethod.addFormParam(formParam.getKey(), param.getParameter());
            } else {
                RepeatedHttpParam params = (RepeatedHttpParam) formParam.getValue();
                for (String param : params.getParameters()) {
                    clientWithResourcePathAndMethod.addFormParam(formParam.getKey(), param);
                }
            }
        }

        for (BodyPart bodyPart : requestBuilder.getMultipartParams()) {

            if (bodyPart.isString()) {
                StringPart part = (StringPart) bodyPart;
                clientWithResourcePathAndMethod
                        .addStringBodyPart(
                                part.getName(),
                                part.getValue(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        );
            }

            if (bodyPart.isFile()) {
                FilePart part = (FilePart) bodyPart;
                clientWithResourcePathAndMethod
                        .addFileBodyPart(
                                part.getName(),
                                part.getFile(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getFileName(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        );
            }

            if(bodyPart.isByteArray()) {
                ByteArrayPart part = (ByteArrayPart) bodyPart;
                clientWithResourcePathAndMethod
                        .addByteArrayBodyPart(
                                part.getName(),
                                part.getBytes(),
                                part.getContentType(),
                                part.getCharset(),
                                part.getContentId(),
                                part.getTransferEncoding()
                        );
            }

        }

        ClientRequest clientRequest = clientWithResourcePathAndMethod.build();

        // ToDo: logging s"client request: $clientRequest"

        /**
         * client.execute[Response[String]](
         clientRequest,
         serverResponse =>
         Response(
         status = serverResponse.getStatusCode,
         stringBody = serverResponse.getResponseBody,
         jsonBody = None,
         body = Some(serverResponse.getResponseBody),
         headers = serverResponse.getHeaders.asScala.foldLeft(Map.empty[String, List[String]]) { (map, el) =>
         val (key, value) = el
         map + (key -> value.asScala.toList)
         }
         )
         )
         */
        client.execute(clientRequest, (response) -> {
            return new Response(null, response.getHeaders(), response.getStatusCode(), response.getResponseBody());
        } );
    }

    @Override
    public <B> Future<Response<String>> callToStringResponse(RequestBuilder request, B body) {
        return null;
    }

    @Override
    public <B, R> Future<Response<R>> callToTypeResponse(RequestBuilder request, B body) {
        return null;
    }

    @Override
    public void close() {

    }

}
