package com.miao.tlv;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

@Slf4j
public class StringsTest {

    @Test
    public void testByteArrayToHex() {
        int[] input = new int[]{0xA1, 0xA4, 0xA9};

        byte[] convertedBytes = Strings.hexToByteArray(input);
        Assert.assertEquals("should be equals to", "[-95, -92, -87]", Arrays.toString(convertedBytes));

        byte[] bytes = new byte[]{-95, -92, -87};
        int[] convertedIntArray = Strings.byteArrayToIntArray(bytes);
        Assert.assertEquals("should be equals to", "[161, 164, 169]", Arrays.toString(convertedIntArray));

        String[] convertedHexArray = Strings.byteArrayToHexArray(bytes);
        Assert.assertEquals("should be equals to", "[0xA1, 0xA4, 0xA9]", Arrays.toString(convertedHexArray));

    }

    @Test
    public void testPrintTT() {
        int t1 = 255;
        String n1 = Strings.printTT(t1);
        log.info("n1: {}", n1);
        log.info("n1 in hex - {}", Strings.printTT(0x00));

        Assert.assertTrue(Strings.printTT(0x00).equals("0x00"));
        Assert.assertTrue(Strings.printTT(0).equals("0x00"));

        Assert.assertTrue(Strings.printTT(0xFC).equals("0xFC"));
        Assert.assertTrue(Strings.printTT(252).equals("0xFC"));

        Assert.assertTrue(Strings.printTT(0xFD).equals("0x00FD"));
        Assert.assertTrue(Strings.printTT(253).equals("0x00FD"));

        Assert.assertTrue(Strings.printTT(0x100).equals("0x0100"));
        Assert.assertTrue(Strings.printTT(256).equals("0x0100"));

        Assert.assertTrue(Strings.printTT(0xFFFF).equals("0xFFFF"));
        Assert.assertTrue(Strings.printTT(65535).equals("0xFFFF"));

        Assert.assertTrue(Strings.printTT(0xFFFFFFFF).equals("0xFFFFFFFF"));
    }

}
