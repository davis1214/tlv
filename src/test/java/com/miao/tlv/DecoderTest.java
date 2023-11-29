package com.miao.tlv;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DecoderTest {

    private Encoder encoder;

    private byte[] output;

    @Before
    public void before() {
        encoder = new Encoder();
    }

    @Test
    public void testDecode() {
        int type = 12;
        byte[] value = new byte[]{12, 2, 20};
        TLV tlv = new TLV(type, value);
        encoder.prependTlv(tlv);
        int tlvPosition = tlv.getValueBuf().position();

        Decoder decoder = new Decoder(encoder.getWriteBuf());
        TLV decodeTlv = decoder.read();
        System.out.println("tlv->" + tlv);

        // decode 不影响原 encoder byteBuffer position
        Assert.assertTrue(tlvPosition == tlv.getValueBuf().position());

        // decode 不影响原 encoder byteBuffer position
        Assert.assertTrue(tlvPosition == tlv.getValueBuf().position());
        Assert.assertTrue(tlv.getType() == decodeTlv.getType());
        Assert.assertTrue(tlv.getLength() == decodeTlv.getLength());
        Assert.assertTrue(Arrays.toString(tlv.getValueBuf().array()).equals(Arrays.toString(decodeTlv.getValueBuf().array())));

        Assert.assertTrue(decodeTlv.getChildTlvList() == null || decodeTlv.getChildTlvList().size() == 0);
    }


    @Test
    public void testNestedTLV() {
        // actual: T1-L1-[T2-L2-V2, T3-L3-[TL4-L4-V4]]

        encoder.prependTlv(buildNestedTLV());
        Decoder decoder = new Decoder(encoder.getWriteBuf());
        TLV decodeTlv = decoder.read();
        System.out.println("tlv->" + decodeTlv);

        Assert.assertTrue(decodeTlv.getType() == 12);
        List<TLV> tlvList = decodeTlv.getChildTlvList();
        Assert.assertTrue(tlvList != null && tlvList.size() == 2);

        Assert.assertTrue(tlvList.get(0).getType() == 253);

        // Arrays.toString(encoder.getWriteBuf().array()))
        Assert.assertTrue(Arrays.toString(tlvList.get(0).getValue()).equals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"));
        Assert.assertTrue(tlvList.get(1).getType() == 345);

        TLV tlv3 = tlvList.get(1);

        List<TLV> tlvList2 = tlv3.getChildTlvList();
        Assert.assertTrue(tlvList2.size() == 1);

        Assert.assertTrue(tlvList2.get(0).getType() == 35);
        Assert.assertTrue(Arrays.toString(tlvList2.get(0).getValue()).equals("[52, 62, 72, 82, 92]"));
    }

    @Test
    public void testReadTlvList() {
        TLV tlv1 = new TLV(12, new byte[]{12, 2});
        TLV tlv2 = new TLV(253, new byte[]{123, 3});

        // 此处编码后，和encoder.getWriteBuf 不是一个对象
        byte[] tlvBytes = encoder.encode(Lists.newArrayList(tlv1, tlv2), 10);

        Decoder decoder = new Decoder(ByteBuffer.wrap(tlvBytes));
        // 1 TLV tlv = decoder.read();
        List<TLV> tlvList = decoder.readTlvList();

        Assert.assertTrue(String.format("tlvList.size: %s", tlvList.size()), tlvList.size() == 2);
        Assert.assertTrue(tlvList.get(0).getType() == 12);
        Assert.assertTrue(tlvList.get(1).getType() == 253);

        Assert.assertTrue(Arrays.toString(tlvList.get(0).getValue()).equals("[12, 2]"));
        Assert.assertTrue(Arrays.toString(tlvList.get(1).getValue()).equals("[123, 3]"));
    }

    private TLV buildNestedTLV() {
        // 嵌套的结构： T1-L1-[T2-L2-V2, T3-L3-[TL4-L4-V4]]
        // actual: 12-[253-{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 345-{35-{52, 62, 72, 82, 92}}]

        // tl[tl[tl[tlv]]]

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

        return tlv1;
    }


}
