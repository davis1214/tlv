package com.miao.tlv;

public interface Decodable<R> {
    R decodeFrom(Decoder decoder);
}
