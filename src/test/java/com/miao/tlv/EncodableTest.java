package com.miao.tlv;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

@Slf4j
public class EncodableTest {
    class NameEncodable implements Encodable {
        @Override
        public void encodeTo(Encoder encoder) {
            TLV tlv = new TLV(12, new byte[]{12, 2});
            encoder.prependTlv(tlv);
        }
    }

    @Test
    public void testEncodable() {
        NameEncodable nameEncodable = new NameEncodable();
        byte[] output = Encoder.encode(nameEncodable);

        System.out.println("buffer str: " + Arrays.toString(output));
        Assert.assertTrue("[12, 2, 12, 2]".equals(Arrays.toString(output)));
    }

    // extract 注入了一个回调函数


}
