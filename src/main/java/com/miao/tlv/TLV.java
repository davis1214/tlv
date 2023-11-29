package com.miao.tlv;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@Slf4j
public class TLV {

    /**
     * tlv-type
     */
    @Getter
    private long type;

    /**
     * tlv-length
     */
    private int length;

    /**
     * buf is null if not leaf node
     */
    @Getter
    private ByteBuffer valueBuf;

    /**
     * tlv list
     */
    @Getter
    private List<TLV> tlvList;

    @Getter
    private Decoder decoder;

    // 在扩展的编码实现（Encodable）中，需要增加校验，某些类型的数据，不进行编码

    public TLV(long type, int length) {
        this.type = type;
        this.length = length;

        if (length > 0 && valueBuf == null) {
            log.warn("tlv-length is greater than zero, but no buf found");
        }
    }

    public TLV(long type, byte[] bytes) {
        this.type = type;
        this.length = bytes.length;
        this.valueBuf = ByteBuffer.wrap(bytes);
    }

    public TLV(long type, TLV tlv) {
        this.type = type;
        if (tlv == null) {
            return;
        }

        this.length = tlv.getLength();
        addChildTLV(tlv);
    }

    public TLV(long type, ByteBuffer valueBuf) {
        this.type = type;
        this.length = length;
        this.valueBuf = valueBuf;
    }

    public TLV(long type, int length, ByteBuffer valueBuf, Decoder decoder) {
        this.type = type;
        this.length = length;
        this.valueBuf = valueBuf;
        this.decoder = decoder;
    }

    public byte[] getValue() {
        return Strings.getRemainBytes(valueBuf);
    }

    public List<TLV> getChildTlvList() {
        if (this.valueBuf == null || this.valueBuf.remaining() == 0) {
            return null;
        }

        List<TLV> tlvList = Lists.newArrayList();
        while (valueBuf.position() < valueBuf.limit()) {
            int type = readVarNum();
            int length = readVarNum();

            // TODO 此处有bug
            if (valueBuf.remaining() < length) {
                break;
            }

            if (length == 0) {
                tlvList.add(new TLV(type, length));
            } else {
                byte[] value = new byte[length];
                valueBuf.get(value);
                tlvList.add(new TLV(type, value));
            }
        }

        this.valueBuf.flip();
        return tlvList;
    }

//    public TLV read() {
//        int type = readVarNum();
//        int length = readVarNum();
//        if (length == 0) {
//            return new TLV(type, length);
//        }
//
//        byte[] value = new byte[length];
//        valueBuf.get(value);
//        return new TLV(type, ByteBuffer.wrap(value));
//    }

    public int readVarNum() {
        if (valueBuf.position() >= valueBuf.limit()) {
            return 0;
        }

        int firstOctet = (int) valueBuf.get() & 0XFF;
        if (firstOctet < 0XFD) {
            return firstOctet;
        } else if (firstOctet == 0XFD) {
            return (((int) valueBuf.get() & 0XFF) << 8)
                    + ((int) valueBuf.get() & 0XFF);
        } else if (firstOctet == 0XFE) {
            return (((int) valueBuf.get() & 0XFF) << 24)
                    + (((int) valueBuf.get() & 0XFF) << 16)
                    + (((int) valueBuf.get() & 0XFF) << 8)
                    + ((int) valueBuf.get() & 0XFF);
        }

        throw new RuntimeException(" 64-bit VAR-NUMBER is not supported");
    }

    public int addChildTLV(TLV tlv) {
        if (this.tlvList == null) {
            this.tlvList = Lists.newArrayList();
        }

        this.tlvList.add(tlv);
        this.valueBuf = null;
        return tlv.getTlvSize();
    }

    public int getTlvSize() {
        int typeLengthSize = Strings.sizeofVarNum(this.type) + Strings.sizeofVarNum(this.length);

        if (this.tlvList == null) {
            int bufSize = this.valueBuf == null ? 0 : this.valueBuf.remaining();
            return typeLengthSize + bufSize;
        }
        return typeLengthSize + getTlvSize(this.tlvList);
    }


    public Decoder vd() {
        if(this.valueBuf == null){
            this.valueBuf = ByteBuffer.wrap(new byte[0]);
        }
        return new Decoder(this.valueBuf.array());
    }

    private int getTlvSize(List<TLV> tlvList) {
        if (tlvList == null || tlvList.size() == 0) {
            return 0;
        }

        int tlvSize = 0;
        for (TLV tlv : tlvList) {
            int typeLengthSize = Strings.sizeofVarNum(tlv.getType()) + Strings.sizeofVarNum(tlv.getLength());
            tlvSize += typeLengthSize;
            if (tlv.getTlvList() == null || tlv.getTlvList().size() == 0) {
                int bufSize = tlv.getValueBuf() == null ? 0 : tlv.getValueBuf().remaining();
                tlvSize += bufSize;
            } else {
                tlvSize += getTlvSize(tlv.getTlvList());
            }
        }
        return tlvSize;
    }

    public int getLength() {
        if (this.tlvList != null && this.tlvList.size() > 0) {
            int tlvSize = getTlvSize();
            return tlvSize - Strings.sizeofVarNum(this.type) - Strings.sizeofVarNum(tlvSize);
        } else if (this.valueBuf != null) {
            return this.valueBuf.remaining();
        }

        return this.length;
    }

    @Override
    public String toString() {
        return "EncodeTlv{" +
                "type=" + this.type +
                ", length=" + this.length +
                ", buf=" + (this.valueBuf == null ? "null" : Arrays.toString(valueBuf.array())) +
                '}';
    }

}
