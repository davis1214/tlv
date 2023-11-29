package com.miao.tlv.materials;

import org.junit.Test;

public class AssertTest {

    @Test
    public void testAssert() {
        // if v = false, there is error would throw
        boolean v = true;
        assert v : "it should be true";
    }

}
