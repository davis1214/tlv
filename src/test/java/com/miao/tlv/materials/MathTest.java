package com.miao.tlv.materials;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class MathTest {

    @Test
    public void testMathTrunc() {
        // System.out.println("math: "+ Math.tr);
    }

    @Test
    public void testMath() {
        double l1 = 12.2123;
        // 12, 12
        System.out.println("int: " + (int) l1 + ", byte: " + (byte) l1);
        Assert.assertTrue(12 == (int) l1);
    }

}
