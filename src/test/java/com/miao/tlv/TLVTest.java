package com.miao.tlv;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class TLVTest {

    @Test
    public void testTLVCreate() {
        TLV tlv1 = new TLV(12, new byte[]{12, 2});
        int size1 = tlv1.getTlvSize();
        Assert.assertTrue("tlvSize should be 4", size1 == 4);

        //  2 + 1 + 10
        TLV tlv2 = new TLV(253, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        int size2 = tlv2.getTlvSize();
        Assert.assertTrue(String.format("tlvSize should be 14 - current %s", size2), size2 == 14);
    }

    /**
     * nestedTLV
     * T1-L1-[T2-L2-V2]
     */
    @Test
    public void testNestedTLV() {

        TLV tlv1 = new TLV(12, new byte[]{12, 2});
        int size1 = tlv1.getTlvSize();
        Assert.assertTrue("tlvSize should be 4", size1 == 4);

        //  2 + 1 + 10
        TLV tlv2 = new TLV(253, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        int size2 = tlv2.getTlvSize();
        Assert.assertEquals(String.format("tlvSize should be 14 - current %s", size2), 14, size2);

        tlv1.addChildTLV(tlv2);

        int size12 = tlv1.getTlvSize();
        Assert.assertTrue(String.format("tlvSize should be 16 - current %s", size2), size12 == 16);
    }


    /**
     * TODO NEW
     */
    @Test
    public void testNestedTLV2() {
        // 嵌套的结构： T1-L1-[T2-L2-V2, T3-L3-[TL4-L4-V5]]
        TLV tlv1 = new TLV(12, new byte[]{12, 2});
        int size1 = tlv1.getTlvSize();
        Assert.assertTrue("tlv1 Size should be 4", size1 == 4);

        //  3 + 1 + 10
        TLV tlv2 = new TLV(253, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        int size2 = tlv2.getTlvSize();
        Assert.assertTrue(String.format("tlv2 Size should be 14 - current %s", size2), size2 == 14);
        tlv1.addChildTLV(tlv2);

        //  3 + 1
        TLV tlv3 = new TLV(345, 1);
        int size3 = tlv3.getTlvSize();
        Assert.assertTrue(String.format("tlv3 Size should be 4 - current %s", size3), size3 == 4);

        //  1 + 1 + 5
        TLV tlv4 = new TLV(35, new byte[]{52, 62, 72, 82, 92});
        int size4 = tlv4.getTlvSize();
        Assert.assertTrue(String.format("tlv4 Size should be 7 - current %s", size4), size4 == 7);

        tlv3.addChildTLV(tlv4);
        tlv1.addChildTLV(tlv3);

        // size: 1 - 1 - [ 14 , 3 - 1 - [ 1 - 1 - 5 ] ]
        int tlvSize = tlv1.getTlvSize();
        Assert.assertTrue(String.format("tlvSize should be 27 - current %s", tlvSize), tlvSize == 27);
    }


//    @Test
//    public void testReservedTLV() {
//        TLV tlv1 = new TLV(12, 2, new byte[]{12, 2});
//        int size1 = tlv1.getTlvSize();
//        Assert.assertTrue("tlvSize should be 4", size1 == 4);
//
//        //  2 + 1 + 10
//        TLV tlv2 = new TLV(253, 2, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
//        int size2 = tlv2.getTlvSize();
//        Assert.assertTrue(String.format("tlvSize should be 14 - current %s", size2), size2 == 14);
//        tlv1.setTlv(tlv2);
//
//        TLV tlv3 = new TLV(65535, 2, new byte[]{11, 22, 33, 44, 55});
//        int size3 = tlv3.getTlvSize();
//        Assert.assertTrue(String.format("tlvSize should be 9 - current %s", size3), size3 == 9);
//
//        tlv2.setTlv(tlv3);
//        tlv1.setTlv(tlv2);
//
//        int size12 = tlv1.getTlvSize();
//        Assert.assertTrue(String.format("tlvSize should be 15 - current %s", size12), size12 == 15);
//
//
//        TLV reversedTlv = tlv1.getReverseTLV();
//        Assert.assertTrue(String.format("reversed tlv-type should be 65535 - current %s", reversedTlv.getType()), reversedTlv.getType() == 65535);
//        Assert.assertTrue(reversedTlv.getTlv() != null && reversedTlv.getTlv().getType() == 253);
//        Assert.assertTrue(reversedTlv.getTlv().getTlv() != null && reversedTlv.getTlv().getTlv().getType() == 12);
//        Assert.assertTrue(reversedTlv.getTlv().getTlv().getTlv() == null);
//    }


}
