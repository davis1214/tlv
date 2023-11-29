package com.miao.tlv.enums;

public enum Len {
    LEN_1(1),
    LEN_2(2),
    LEN_4(4),
    LEN_8(8);

    private final int index;

    Len(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
