/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson.client;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.nio.charset.Charset;

/**
 * Created by peter on 18/09/15.
 */
public class ClientConfig {

    private Charset requestCharset = Charset.defaultCharset();
    private Charset responseCharset = Charset.defaultCharset();
    private int requestTimeout = 60000;
    private int maxRequestRetry = 5;
    private int connectTimeout = 50000;
    private int connectionTTL = -1;
    private int readTimeout = 60000;
    private int webSocketTimeout = 900000;
    private int maxConnections = -1;
    private int maxConnectionsPerHost = -1;
    private Boolean allowPoolingConnections = true;
    private Boolean allowPoolingSslConnections = true;
    private int pooledConnectionIdleTimeout = 60000;
    private Boolean acceptAnyCertificate = false;
    private Boolean followRedirect = true;
    private int maxRedirects = 5;
    private Boolean strict302Handling = false;
    private Integer sslSessionTimeout = 900000;
    private Integer sslSessionCacheSize = null;
    private SSLContext sslContext = null;
    private HostnameVerifier hostnameVerifier = null;

    public ClientConfig() {
    }

    public ClientConfig(Charset requestCharset,
                        Charset responseCharset,
                        Boolean acceptAnyCertificate,
                        Boolean allowPoolingConnections,
                        Boolean allowPoolingSslConnections,
                        int connectionTTL,
                        int connectTimeout,
                        Boolean followRedirect,
                        int maxConnections,
                        int maxConnectionsPerHost,
                        int maxRedirects,
                        int maxRequestRetry,
                        int pooledConnectionIdleTimeout,
                        int readTimeout,
                        Boolean removeQueryParamOnRedirect,
                        int requestTimeout,
                        Boolean strict302Handling,
                        int webSocketTimeout) {

        this.requestCharset = requestCharset;
        this.responseCharset = responseCharset;
        this.acceptAnyCertificate = acceptAnyCertificate;
        this.allowPoolingConnections = allowPoolingConnections;
        this.allowPoolingSslConnections = allowPoolingSslConnections;
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
        this.webSocketTimeout = webSocketTimeout;
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

    public Boolean getAcceptAnyCertificate() {
        return acceptAnyCertificate;
    }

    public void setAcceptAnyCertificate(Boolean acceptAnyCertificate) {
        this.acceptAnyCertificate = acceptAnyCertificate;
    }

    public Boolean getAllowPoolingConnections() {
        return allowPoolingConnections;
    }

    public void setAllowPoolingConnections(Boolean allowPoolingConnections) {
        this.allowPoolingConnections = allowPoolingConnections;
    }

    public Boolean getAllowPoolingSslConnections() {
        return allowPoolingSslConnections;
    }

    public void setAllowPoolingSslConnections(Boolean allowPoolingSslConnections) {
        this.allowPoolingSslConnections = allowPoolingSslConnections;
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

    public int getWebSocketTimeout() {
        return webSocketTimeout;
    }

    public void setWebSocketTimeout(int webSocketTimeout) {
        this.webSocketTimeout = webSocketTimeout;
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

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }
}