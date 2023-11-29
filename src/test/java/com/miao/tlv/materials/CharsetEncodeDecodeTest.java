package com.miao.tlv.materials;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class CharsetEncodeDecodeTest {

    @Test
    public void test() {
        //  charset.encode: [104, 101, 108, 108, 111], charset.decode: hello
        Charset charset = Charset.forName("UTF8");
        byte[] bytes = charset.encode("hello").array();
        String str = charset.decode(ByteBuffer.wrap(bytes)).toString();
        System.out.println("charset.encode: " + Arrays.toString(bytes) + ", charset.decode: " + str);

        Assert.assertTrue(bytes.equals("[104, 101, 108, 108, 111]"));
        Assert.assertTrue(str.equals("hello"));
    }

}
