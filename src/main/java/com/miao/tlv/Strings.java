package com.miao.tlv;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * string.ts
 */
public class Strings {

    public static String printTT(int tlvType) {
        // 转为十六进制，并大写
        String s = Integer.toHexString(tlvType).toUpperCase();
        if (tlvType < 0xFD) {
            // 2位，不够前面补0
            return "0x" + String.format("%2s", s).replace(' ', '0');
        }
        if (tlvType <= 0xFFFF) {
            return "0x" + String.format("%4s", s).replace(' ', '0');
        }
        return "0x" + String.format("%8s", s).replace(' ', '0');
    }

    public static byte[] hexToByteArray(int[] input) {
        if (input == null) {
            return new byte[]{};
        }

        byte[] bytes = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            bytes[i] = (byte) input[i];
        }
        return bytes;
    }

    public static int[] byteArrayToIntArray(byte[] input) {
        if (input == null) {
            return new int[]{};
        }

        int[] hex = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            hex[i] = input[i] & 0xFF;
        }
        return hex;
    }

    public static String[] byteArrayToHexArray(byte[] input) {
        if (input == null) {
            return new String[]{};
        }

        String[] hex = new String[input.length];
        for (int i = 0; i < input.length; i++) {
            hex[i] = String.format("0x%2X", input[i] & 0xFF);
        }
        return hex;
    }


    public static byte[] getRemainBytes(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        int position = buffer.position();
        int remaining = buffer.remaining();

        byte[] remainBytes = new byte[remaining];
        byte[] bytes = buffer.array();
        for (int i = position, j = 0; i < bytes.length; i++, j++) {
            remainBytes[j] = bytes[i];
        }

        return remainBytes;
    }

    public static String getRemainBytesString(ByteBuffer buffer) {
        byte[] remainBytes = getRemainBytes(buffer);
        return Arrays.toString(remainBytes);
    }

    public static int sizeofVarNum(long n) {
        // 253
        if (n < 0xFD) {
            return 1;
        }
        // 65535
        if (n <= 0xFFFF) {
            return 3;
        }
        // 4294967295
        if (n <= 0xFFFFFFFF) {
            return 5;
        }
        throw new IllegalArgumentException("VAR-NUMBER is too large");
    }
}
