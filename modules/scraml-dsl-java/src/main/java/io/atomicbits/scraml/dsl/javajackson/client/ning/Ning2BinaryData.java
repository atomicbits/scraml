package io.atomicbits.scraml.dsl.javajackson.client.ning;

import io.atomicbits.scraml.dsl.javajackson.BinaryData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by peter on 08/07/2020.
 */
public class Ning2BinaryData extends BinaryData {

    private org.asynchttpclient.Response innerResponse;

    public Ning2BinaryData(org.asynchttpclient.Response innerResponse) {
        this.innerResponse = innerResponse;
    }

    @Override
    public byte[] asBytes() throws IOException {
        return innerResponse.getResponseBodyAsBytes();
    }

    @Override
    public InputStream asStream() throws IOException {
        return innerResponse.getResponseBodyAsStream();
    }

    @Override
    public String asString() throws IOException {
        return innerResponse.getResponseBody();
    }

    @Override
    public String asString(String charset) throws IOException {
        return innerResponse.getResponseBody(Charset.forName(charset));
    }

}
