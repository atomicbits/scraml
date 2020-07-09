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

package io.atomicbits.scraml.dsl.javajackson.client;

//import com.ning.http.client.AsyncHttpClientConfigDefaults;
import io.netty.handler.ssl.SslContext;
import org.asynchttpclient.config.AsyncHttpClientConfigDefaults;

import javax.net.ssl.HostnameVerifier;
import java.nio.charset.Charset;

/**
 * Created by peter on 18/09/15.
 */
public class ClientConfig {

    private Charset requestCharset = Charset.defaultCharset();
    private Charset responseCharset = Charset.defaultCharset();
    private int requestTimeout = AsyncHttpClientConfigDefaults.defaultRequestTimeout();
    private int maxRequestRetry = AsyncHttpClientConfigDefaults.defaultMaxRequestRetry();
    private int connectTimeout = AsyncHttpClientConfigDefaults.defaultConnectTimeout();
    private int connectionTTL = AsyncHttpClientConfigDefaults.defaultConnectionTtl();
    private int readTimeout = AsyncHttpClientConfigDefaults.defaultReadTimeout();
    private int maxConnections = AsyncHttpClientConfigDefaults.defaultMaxConnections();
    private int maxConnectionsPerHost = AsyncHttpClientConfigDefaults.defaultMaxConnectionsPerHost();
    private int pooledConnectionIdleTimeout = AsyncHttpClientConfigDefaults.defaultPooledConnectionIdleTimeout();
    private Boolean useInsecureTrustManager = AsyncHttpClientConfigDefaults.defaultUseInsecureTrustManager();
    private Boolean followRedirect = AsyncHttpClientConfigDefaults.defaultFollowRedirect();
    private int maxRedirects = AsyncHttpClientConfigDefaults.defaultMaxRedirects();
    private Boolean strict302Handling = AsyncHttpClientConfigDefaults.defaultStrict302Handling();
    private Integer sslSessionTimeout = AsyncHttpClientConfigDefaults.defaultSslSessionTimeout();
    private Integer sslSessionCacheSize = AsyncHttpClientConfigDefaults.defaultSslSessionCacheSize();
    private SslContext sslContext;
    private HostnameVerifier hostnameVerifier;

    public ClientConfig() {
    }

    public ClientConfig(Charset requestCharset,
                        Charset responseCharset,
                        Boolean useInsecureTrustManager,
                        int connectionTTL,
                        int connectTimeout,
                        Boolean followRedirect,
                        int maxConnections,
                        int maxConnectionsPerHost,
                        int maxRedirects,
                        int maxRequestRetry,
                        int pooledConnectionIdleTimeout,
                        int readTimeout,
                        int requestTimeout,
                        Boolean strict302Handling) {

        this.requestCharset = requestCharset;
        this.responseCharset = responseCharset;
        this.useInsecureTrustManager = useInsecureTrustManager;
        this.connectionTTL = connectionTTL;
        this.connectTimeout = connectTimeout;
        this.followRedirect = followRedirect;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.maxRedirects = maxRedirects;
        this.maxRequestRetry = maxRequestRetry;
        this.pooledConnectionIdleTimeout = pooledConnectionIdleTimeout;
        this.readTimeout = readTimeout;
        this.requestTimeout = requestTimeout;
        this.strict302Handling = strict302Handling;
    }

    public Charset getRequestCharset() {
        return requestCharset;
    }

    public void setRequestCharset(Charset requestCharset) {
        this.requestCharset = requestCharset;
    }

    public Charset getResponseCharset() {
        return responseCharset;
    }

    public void setResponseCharset(Charset responseCharset) {
        this.responseCharset = responseCharset;
    }

    public Boolean getUseInsecureTrustManager() {
        return useInsecureTrustManager;
    }

    public void setUseInsecureTrustManager(Boolean useInsecureTrustManager) {
        this.useInsecureTrustManager = useInsecureTrustManager;
    }

    public int getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(int connectionTTL) {
        this.connectionTTL = connectionTTL;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Boolean getFollowRedirect() {
        return followRedirect;
    }

    public void setFollowRedirect(Boolean followRedirect) {
        this.followRedirect = followRedirect;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public int getMaxRequestRetry() {
        return maxRequestRetry;
    }

    public void setMaxRequestRetry(int maxRequestRetry) {
        this.maxRequestRetry = maxRequestRetry;
    }

    public int getPooledConnectionIdleTimeout() {
        return pooledConnectionIdleTimeout;
    }

    public void setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
        this.pooledConnectionIdleTimeout = pooledConnectionIdleTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Boolean getStrict302Handling() {
        return strict302Handling;
    }

    public void setStrict302Handling(Boolean strict302Handling) {
        this.strict302Handling = strict302Handling;
    }

    public Integer getSslSessionTimeout() {
        return sslSessionTimeout;
    }

    public void setSslSessionTimeout(Integer sslSessionTimeout) {
        this.sslSessionTimeout = sslSessionTimeout;
    }

    public Integer getSslSessionCacheSize() {
        return sslSessionCacheSize;
    }

    public void setSslSessionCacheSize(Integer sslSessionCacheSize) {
        this.sslSessionCacheSize = sslSessionCacheSize;
    }

    public SslContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }
}
