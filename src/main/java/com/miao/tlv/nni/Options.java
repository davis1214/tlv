package com.miao.tlv.nni;

import com.miao.tlv.enums.Len;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Options {

    private Len len;

    private boolean unsafe = false;

    private boolean big = false;

    public static Options of(Len len) {
        return of(len, false, false);
    }

    public static Options of(Len len, boolean unsafe) {
        return of(len, unsafe, false);
    }

    public static Options of(Len len, boolean unsafe, boolean big) {
        return new Options(len, unsafe, big);
    }

}

