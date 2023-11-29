package com.miao.tlv.nni;

import com.miao.tlv.Commons;
import com.miao.tlv.Encodable;
import com.miao.tlv.Encoder;
import com.miao.tlv.enums.EncodeNniClass;
import com.miao.tlv.enums.Len;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.miao.tlv.Commons.MAX_SAFE_INTEGER;
import static com.miao.tlv.Commons._NUMBER_FORMAT;

/**
 * 非负整数编解码（ 构造方法 & decode 方法）
 */
public class Nni {

//    public static boolean isValidLength(int len) {
//        return EncodeNniClass.contains(len);
//    }
//
//    public static int constrain(Number n, String typeName) {
//        return constrain(n, typeName, 0, Integer.MAX_VALUE);
//    }
//
//    public static int constrain(Number n, String typeName, int max) {
//        return constrain(n, typeName, 0, max);
//    }

    /**
     * 返回数值表达式 x 的整数部分，删除所有小数位。如果 x 已经是整数，则结果为 x
     *
     * @param n
     * @param typeName
     * @param min
     * @param max
     * @return
     */
    public static int constrain(Number n, String typeName, int min, int max) {
        if (!(n.doubleValue() >= min && n.doubleValue() <= max)) {
            throw new IllegalArgumentException(n + " is out of " + typeName + " valid range");
        }
        return n.intValue();
    }


    /**
     * 解码
     *
     * @param buffer
     * @return Number
     */
    public static Number decode(ByteBuffer buffer) {
        return decode(buffer, null);
    }

    /**
     * 解码
     *
     * @param buffer
     * @param options
     * @return
     */
    public static Number decode(ByteBuffer buffer, Options options) {
        if (options != null && options.getLen() != null && options.getLen().getIndex() != buffer.remaining()) {
            throw new IllegalArgumentException(String.format("incorrect TLV-LENGTH of NNI%s", options.getLen().getIndex()));
        }

        if (options != null && options.isBig()) {
            if (buffer.remaining() == 8) {
                return (((long) buffer.get() & 0xff) << 56) +
                        (((long) buffer.get() & 0xff) << 48) +
                        (((long) buffer.get() & 0xff) << 40) +
                        (((long) buffer.get() & 0xff) << 32) +
                        (((long) buffer.get() & 0xff) << 24) +
                        (((long) buffer.get() & 0xff) << 16) +
                        (((long) buffer.get() & 0xff) << 8) +
                        ((long) buffer.get() & 0xff);
            } else {
                // 改为上使用 bigInteger 解码
                return (((long) buffer.get() & 0xff) << 56) +
                        (((long) buffer.get() & 0xff) << 48) +
                        (((long) buffer.get() & 0xff) << 40) +
                        (((long) buffer.get() & 0xff) << 32) +
                        (((long) buffer.get() & 0xff) << 24) +
                        (((long) buffer.get() & 0xff) << 16) +
                        (((long) buffer.get() & 0xff) << 8) +
                        ((long) buffer.get() & 0xff);
            }
        }

        if (buffer.remaining() == 8) {
            // bigInteger 类型解码
            Number number = (((long) buffer.get() & 0xff) << 56) +
                    (((long) buffer.get() & 0xff) << 48) +
                    (((long) buffer.get() & 0xff) << 40) +
                    (((long) buffer.get() & 0xff) << 32) +
                    (((long) buffer.get() & 0xff) << 24) +
                    (((long) buffer.get() & 0xff) << 16) +
                    (((long) buffer.get() & 0xff) << 8) +
                    ((long) buffer.get() & 0xff);

            boolean unsafe = options != null ? options.isUnsafe() : false;
            if (!unsafe && !isSafeInteger(number.longValue())) {
                throw new IllegalArgumentException(String.format("NNI is too large %s", number.doubleValue()));
            }

            return number;
        }

        return decode32(buffer);
    }

    private static Number decode32(ByteBuffer buffer) {
        switch (buffer.remaining()) {
            case 1:
                return (long) buffer.get() & 0xff;
            case 2:
                return (((long) buffer.get() & 0xff) << 8) +
                        ((long) buffer.get() & 0xff);
            case 4:
                return (((long) buffer.get() & 0xff) << 24) +
                        (((long) buffer.get() & 0xff) << 16) +
                        (((long) buffer.get() & 0xff) << 8) +
                        ((long) buffer.get() & 0xff);
        }

        throw new IllegalArgumentException("incorrect TLV-LENGTH of NNI");
    }

    /**
     * ts 中的 Integer.MAX_SAFE_INTEGER = 9007199254740991
     * java 中的 Integer.MAX_VALUE = 2147483647
     * <p>
     * BigInteger 有自己的计算方式
     *
     * @param n
     * @return
     */
    private static boolean isSafeBigInteger(BigInteger n) {
        BigInteger maxSafeInteger = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger minSafeInteger = BigInteger.valueOf(Long.MIN_VALUE);
        return n.compareTo(maxSafeInteger) <= 0 && n.compareTo(minSafeInteger) >= 0;
    }

    /**
     * 编码
     *
     * @param n
     * @return
     */
    public static Encodable NNI(Number n) {
        return NNI(n, null);
    }

    /**
     * 编码
     *
     * @param n
     * @param options
     * @return
     */
    public static Encodable NNI(Number n, Options options) {
        if (options != null && options.getLen() != null) {
            if (options.getLen() == Len.LEN_8 && n instanceof BigInteger) {
                return new Nni8Big((BigInteger) n);
            }

            switch (options.getLen().getIndex()) {
                case 1:
                    return new Nni1(n.intValue());
                case 2:
                    return new Nni2(n.intValue());
                case 4:
                    return new Nni4(n.longValue());
                case 8:
                    return new Nni8Number(n.longValue());
                default:
                    throw new IllegalArgumentException(String.format("invalid len - %s", options.getLen().getIndex()));
            }
        }

        if (n instanceof BigInteger) {
            BigInteger bigInt = (BigInteger) n;
            // 0x100000000L = 4294967296 (42 9496 7296)
            if (bigInt.compareTo(BigInteger.valueOf(0x100000000L)) < 0) {
                n = bigInt.longValue();
            } else if (bigInt.compareTo(new BigInteger("18446744073709552000")) <= 0) {  // 0xFFFFFFFFFFFFFFFFn = 18446744073709552000
                return new Nni8Big(bigInt);
            } else {
                throw new IllegalArgumentException("NNI is too large");
            }
        }

        long longValue = n.longValue();
        boolean unsafe = options != null ? options.isUnsafe() : false;

        if (longValue < 0) {
            throw new IllegalArgumentException("NNI cannot be negative");
        } else if (longValue < 0x100) { // 0x100 = 256
            return new Nni1((int) n);
        } else if (longValue < 0x10000) { // 0x10000 = 65536
            return new Nni2((int) n);
        } else if (longValue < 0x100000000L) { // 0x100000000L = 4294967296 , 注 Long.Max_VALUE = 9223372036854775807
            return new Nni4(longValue);
        }

        // 0xFFFFFFFFFFFFFFFFL = 18446744073709552000 为 Long 字节数组的值
        // Long max: 9223372036854775807, Long min: -9223372036854775808
        BigInteger bigIntegerNum = new BigInteger(_NUMBER_FORMAT.format(n));
        if (bigIntegerNum.compareTo(unsafe ? Commons.MAX_UNSAFE_INTEGER : BigInteger.valueOf(Commons.MAX_SAFE_INTEGER)) <= 0) { //  0xFFFFFFFFFFFFFFFF = 18446744073709552000
            return new Nni8Number(longValue);
        }

        throw new IllegalArgumentException("NNI is too large");
    }

    /**
     * Number.MAX_SAFE_INTEGER in ts is 9007199254740991L
     * Long in java max is 9223372036854775807,  min is -9223372036854775808
     */
    private static boolean isSafeInteger(Long n) {
        return n <= MAX_SAFE_INTEGER;
    }
}

class Nni1 implements Encodable {
    private int n;

    public Nni1(int n) {
        this.n = n;
    }

    @Override
    public void encodeTo(Encoder encoder) {
        int position = encoder.getWritePosition(EncodeNniClass.Nni1.getIndex());
        encoder.getWriteBuf().put(position, (byte) (n & 0xff));
    }
}

class Nni2 implements Encodable {
    private int n;

    public Nni2(int n) {
        this.n = n;
    }

    @Override
    public void encodeTo(Encoder encoder) {
        int position = encoder.getWritePosition(EncodeNniClass.Nni2.getIndex());
        encoder.getWriteBuf().put(position, (byte) ((n >> 8) & 0xff));
        encoder.getWriteBuf().put(position + 1, (byte) (n & 0xff));
    }

}

class Nni4 implements Encodable {
    private long n;

    public Nni4(long n) {
        this.n = n;
    }

    @Override
    public void encodeTo(Encoder encoder) {
        int position = encoder.getWritePosition(EncodeNniClass.Nni4.getIndex());
        encoder.getWriteBuf().put(position, (byte) ((n >> 24) & 0xff));
        encoder.getWriteBuf().put(position + 1, (byte) ((n >> 16) & 0xff));
        encoder.getWriteBuf().put(position + 2, (byte) ((n >> 8) & 0xff));
        encoder.getWriteBuf().put(position + 3, (byte) (n & 0xff));
    }
}

class Nni8Number implements Encodable {
    private long n;

    public Nni8Number(long n) {
        this.n = n;
    }

    @Override
    public void encodeTo(Encoder encoder) {
        int position = encoder.getWritePosition(EncodeNniClass.Nni8.getIndex());
        encoder.getWriteBuf().put(position, (byte) ((n >> 56) & 0xff));
        encoder.getWriteBuf().put(position + 1, (byte) ((n >> 48) & 0xff));
        encoder.getWriteBuf().put(position + 2, (byte) ((n >> 40) & 0xff));
        encoder.getWriteBuf().put(position + 3, (byte) ((n >> 32) & 0xff));
        encoder.getWriteBuf().put(position + 4, (byte) ((n >> 24) & 0xff));
        encoder.getWriteBuf().put(position + 5, (byte) ((n >> 16) & 0xff));
        encoder.getWriteBuf().put(position + 6, (byte) ((n >> 8) & 0xff));
        encoder.getWriteBuf().put(position + 7, (byte) (n & 0xff));
    }

}

class Nni8Big implements Encodable {
    private long n;

    public Nni8Big(BigInteger n) {
        this.n = n.longValue();
    }

    @Override
    public void encodeTo(Encoder encoder) {
        int position = encoder.getWritePosition(EncodeNniClass.Nni8.getIndex());
        encoder.getWriteBuf().put(position, (byte) ((n >> 56) & 0xff));
        encoder.getWriteBuf().put(position + 1, (byte) ((n >> 48) & 0xff));
        encoder.getWriteBuf().put(position + 2, (byte) ((n >> 40) & 0xff));
        encoder.getWriteBuf().put(position + 3, (byte) ((n >> 32) & 0xff));
        encoder.getWriteBuf().put(position + 4, (byte) ((n >> 24) & 0xff));
        encoder.getWriteBuf().put(position + 5, (byte) ((n >> 16) & 0xff));
        encoder.getWriteBuf().put(position + 6, (byte) ((n >> 8) & 0xff));
        encoder.getWriteBuf().put(position + 7, (byte) (n & 0xff));
    }
}


