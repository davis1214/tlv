package com.miao.tlv;

import java.math.BigInteger;
import java.text.NumberFormat;

public class Commons {

    /**
     * 默认的 ByteBuffer 大小
     */
    public static final Integer _DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Number.MAX_SAFE_INTEGER in ts is 9007199254740991L
     * Long in java max is 9223372036854775807,  min is -9223372036854775808
     */
    public static final Long MAX_SAFE_INTEGER = 9007199254740991L;

    /**
     * Number.MAX_UNSAFE_INTEGER in ts is 0xFFFFFFFFFFFFFFFFL (18446744073709552000)
     * Long in java max is 9223372036854775807,  min is -9223372036854775808
     */
    public static final BigInteger MAX_UNSAFE_INTEGER = new BigInteger("18446744073709552000");

    /**
     * 格式化数字，取消科学计算书
     */
    public static final NumberFormat _NUMBER_FORMAT = NumberFormat.getInstance();

    static {
        //设置保留多少位小数
        _NUMBER_FORMAT.setMaximumFractionDigits(3);
        // 取消科学计数法
        _NUMBER_FORMAT.setGroupingUsed(false);
    }

}
