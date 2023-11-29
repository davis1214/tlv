package com.miao.tlv;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class DecodableTest {

    class NameDecodable implements Decodable {
        @Override
        public Object decodeFrom(Decoder decoder) {
            TLV tlv = decoder.read();
            return tlv;
        }
    }

    @Test
    public void testDecodable() {
        NameDecodable decodable = new NameDecodable();
//        Decoder.

//        EVD.decode(interest[FIELDS], decoder);

    }



}
