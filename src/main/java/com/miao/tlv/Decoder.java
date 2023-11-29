package com.miao.tlv;

import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Decoder<R> {

    private ByteBuffer readBuf;

    public Decoder(byte[] input) {
        this(input, 0);
    }

    public Decoder(byte[] input, int position) {
        this.readBuf = ByteBuffer.wrap(input);
        this.readBuf.position(position);
    }

    public Decoder(ByteBuffer buffer) {
        this.readBuf = buffer.duplicate();
    }

    /**
     * Determine whether end of input has been reached.
     *
     * @return
     */
    public boolean eof() {
        return this.readBuf.position() >= this.readBuf.limit();
    }

    public int position() {
        return this.readBuf.position();
    }

    public void position(int position) {
        this.readBuf.position(position);
    }

    public R decode(Decodable<R> decodable) {
        return decodable.decodeFrom(this);
    }

    public List<TLV> readTlvList() {
        List<TLV> tlvList = Lists.newArrayList();
        TLV tlv = null;
        while ((tlv = read()) != null) {
            tlvList.add(tlv);
        }
        return tlvList;
    }

    public TLV read() {
        if (this.readBuf.position() >= this.readBuf.limit()) {
            return null;
        }

        int offsetT = this.readBuf.position();
        int type = readVarNum();
        int length = readVarNum();
        if (length == 0) {
            return new TLV(type, length);
        }

        byte[] value = new byte[length];
        readBuf.get(value);

        int offsetE = this.readBuf.position();
        byte[] tlvBytes = Arrays.copyOfRange(readBuf.array(), offsetT, offsetE);
        return new TLV(type, length, ByteBuffer.wrap(value), new Decoder(tlvBytes));
    }

    private int readVarNum() {
        int firstOctet = (int) readBuf.get() & 0XFF;
        if (firstOctet < 0XFD) {
            return firstOctet;
        } else if (firstOctet == 0XFD) {
            return (((int) readBuf.get() & 0XFF) << 8)
                    + ((int) readBuf.get() & 0XFF);
        } else if (firstOctet == 0XFE) {
            return (((int) readBuf.get() & 0XFF) << 24)
                    + (((int) readBuf.get() & 0XFF) << 16)
                    + (((int) readBuf.get() & 0XFF) << 8)
                    + ((int) readBuf.get() & 0XFF);
        }
        throw new RuntimeException(" 64-bit VAR-NUMBER is not supported");
    }
}



