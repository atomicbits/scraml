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
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;

/**
 * Created by peter on 18/09/15.
 */
public class ClientConfig {

    private Charset requestCharset = Charset.defaultCharset();
    private Charset responseCharset = Charset.defaultCharset();
    private int maxRequestRetry = 5;
    private int connectTimeout = 50000;
    private int connectionTTL = -1;
    private int readTimeout = 60000;
    private int writeTimeout = 60000;
    private int maxIdleConnectionsPerHost = 5;

    private Boolean followRedirect = true;
    private SSLContext sslContext = null;
    private X509TrustManager trustManager = null;
    private HostnameVerifier hostnameVerifier = null;

    public ClientConfig() {
    }

    public ClientConfig(Charset requestCharset,
                        Charset responseCharset,
                        int connectionTTL,
                        int connectTimeout,
                        Boolean followRedirect,
                        int maxIdleConnectionsPerHost,
                        int maxRequestRetry,
                        int readTimeout) {

        this.requestCharset = requestCharset;
        this.responseCharset = responseCharset;
        this.connectionTTL = connectionTTL;
        this.connectTimeout = connectTimeout;
        this.followRedirect = followRedirect;
        this.maxIdleConnectionsPerHost = maxIdleConnectionsPerHost;
        this.maxRequestRetry = maxRequestRetry;
        this.readTimeout = readTimeout;
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

    /**
     * Connection TTL in ms
     * -1 means unlimited
     */
    public int getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(int connectionTTL) {
        this.connectionTTL = connectionTTL;
    }

    /**
     * Connection timeout in ms
     */
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


    public int getMaxRequestRetry() {
        return maxRequestRetry;
    }

    public void setMaxRequestRetry(int maxRequestRetry) {
        this.maxRequestRetry = maxRequestRetry;
    }

    /**
     * Read timeout in ms
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
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

    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    public void setTrustManager(X509TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public int getMaxIdleConnectionsPerHost() {
        return maxIdleConnectionsPerHost;
    }

    public void setMaxIdleConnectionsPerHost(int maxIdleConnectionsPerHost) {
        this.maxIdleConnectionsPerHost = maxIdleConnectionsPerHost;
    }

    /**
     * Write timeout in ms
     */
    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
}
