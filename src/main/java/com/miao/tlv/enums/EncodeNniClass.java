package com.miao.tlv.enums;

import java.util.Arrays;

public enum EncodeNniClass {
    Nni1(1),
    Nni2(2),
    Nni4(4),
    Nni8(8),
    ;

    EncodeNniClass(int index) {
        this.index = index;
    }

    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static boolean contains(int index) {
        return Arrays.stream(EncodeNniClass.values()).filter(eNil -> eNil.index == index).count() > 0;
    }


    public static EncodeNniClass of(int index) {
        for (EncodeNniClass encodeNniClass : EncodeNniClass.values()) {
            if (encodeNniClass.getIndex() == index) {
                return encodeNniClass;
            }
        }

        throw new IllegalArgumentException(String.format("invalid len - %s", index));
    }


}
