package com.miao.tlv;

import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

@Slf4j
public class EncoderTest {

    /**
     * 扩容
     */
    @Test
    public void testGrow() {
        Encoder encoder = new Encoder(10);
        Assert.assertTrue(encoder.getSize() == 0);
        Assert.assertTrue(encoder.getWriteBuf().position() == 10);
        Assert.assertTrue(encoder.getWriteBuf().limit() == 10);
        Assert.assertTrue(encoder.getWriteBuf().capacity() == 10);

        ReflectUtil.invoke(encoder, "grow", 11);

        int remainingSize = encoder.getSize();
        log.info("after grow - position: {}, limit: {}, capacity: {}", encoder.getWriteBuf().position(),
                encoder.getWriteBuf().limit(), encoder.getWriteBuf().capacity());
        Assert.assertTrue(encoder.getWriteBuf().position() == 11);
        Assert.assertTrue(encoder.getWriteBuf().limit() == 21);
        Assert.assertTrue(encoder.getWriteBuf().capacity() == 21);
        log.info("remaining size after grwo = {}", remainingSize);
    }

    @Test
    public void testByteBufferPut() {
        // short test
        ByteBuffer bb = ByteBuffer.allocate(2);
        short myShort = (short) 40000;
        bb.putShort(myShort);
        // 9c, 40
        System.out.println(String.format("%02X, %02X", bb.get(0), bb.get(1)));

        // int test
        ByteBuffer bbInt = ByteBuffer.allocate(2);
        bbInt.put((byte) 0xfc);
        System.out.println(bbInt.get(0));
        // FC, 00
        System.out.println(String.format("%02X, %02X", bbInt.get(0), bbInt.get(1)));
    }

    /**
     * prependValue\ prependTypeLength
     */
    @Test
    public void testPrepend() {
        //  actual: type = 252, length = 5, value = {1, 0, 0, 2, 0}
        //  expect: [-4, 5, 1, 0, 0, 2, 0]

        Encoder encoder = new Encoder();
        // 1\prependValue
        // 1, 0, 0, 2, 0
        byte[] value = new byte[]{1, 0, 0, 2, 0};
//        encoder.prependValue(value);
        ReflectUtil.invoke(encoder, "prependValue", value);
        byte[] output = encoder.getOutput();
        System.out.print("bytes = " + Arrays.toString(output));

        // test
        byte[] valueBytes = ByteBuffer.wrap(output, output.length - value.length, value.length).slice().array();
        String array = Arrays.toString(valueBytes);
        System.out.println("prependValue : " + array);

        Assert.assertTrue("[1, 0, 0, 2, 0]".equals(Strings.getRemainBytesString(encoder.getWriteBuf())));

        // 2\prependTypeAndLength
        // 0xfc -> -4
        long tlvType = 252;
        // 0x01 -> 1
        long tlvLength = value.length;

        // 250, 0 -> fa/00 （type/length）
        ReflectUtil.invoke(encoder, "prependTypeLength", tlvType, tlvLength);
        // encoder.prependTypeLength(tlvType, tlvLength);
        output = encoder.getOutput();

        byte[] typeLengthBytes = ByteBuffer.wrap(output, output.length - value.length - 1 - 1, value.length + 1 + 1).array();
        array = Arrays.toString(typeLengthBytes);
        System.out.println("prependTypeLength : " + array);
        //Assert.assertTrue("[0, 0, 0, -4, 5, 1, 0, 0, 2, 0]".equals(Arrays.toString(output)));
        Assert.assertTrue("[-4, 5, 1, 0, 0, 2, 0]".equals(Strings.getRemainBytesString(encoder.getWriteBuf())));

        // remaining, limit, capacity, position
        // pos=3 lim= 10 cap= 10
        System.out.println("buffer : " + encoder.getWriteBuf());
        // Assert.assertTrue(3 == encoder.getBuffer().position());

        byte typeInByte = output[output.length - value.length - 1 - 1];
        System.out.println("type: " + (typeInByte & 0xFF));
        Assert.assertTrue(tlvType == (typeInByte & 0xFF));

        // 验证 解码
        Decoder decoder = new Decoder(encoder.getWriteBuf());
        TLV tlv = decoder.read();
        System.out.println("tlv->" + tlv);

        // 解码后，值与编码前的值相同
        Assert.assertTrue(tlv.getType() == tlvType);
        Assert.assertTrue(tlv.getLength() == tlvLength);
        Assert.assertTrue(Arrays.toString(tlv.getValueBuf().array()).equals(Arrays.toString(value)));
    }

    /**
     * 添加多个 TLV
     */
    @Test
    public void prependMultiTlv() {
        Encoder encoder = new Encoder(10);

        // [pos=3 lim=10 cap=10]
        // [0, 0, 0, -4, 5, 1, 0, 0, 2, 0]
        byte[] value1 = new byte[]{1, 0, 0, 2, 0};
        TLV TLV1 = new TLV(252, ByteBuffer.wrap(value1));
        encoder.prependTlv(TLV1);

        // [pos=3 lim=10 cap=10]
        System.out.println("buffer array: " + encoder.getWriteBuf());
        // [0, 0, 0, -4, 5, 1, 0, 0, 2, 0]
        System.out.println("buffer str: " + Arrays.toString(encoder.getWriteBuf().array()));
        Assert.assertTrue("[0, 0, 0, -4, 5, 1, 0, 0, 2, 0]".equals(Arrays.toString(encoder.getWriteBuf().array())));

        // 扩容 每次都是一个新的Encoder对象
        byte[] value2 = new byte[]{3, 4};
        TLV TLV2 = new TLV(8, ByteBuffer.wrap(value2));
        encoder.prependTlv(TLV2);

        System.out.println("buffer array: " + encoder.getWriteBuf());
        // [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 2, 3, 4, -4, 5, 1, 0, 0, 2, 0]
        System.out.println("buffer str: " + Arrays.toString(encoder.getWriteBuf().array()));
        Assert.assertTrue("[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 2, 3, 4, -4, 5, 1, 0, 0, 2, 0]".equals(Arrays.toString(encoder.getWriteBuf().array())));
    }


    /**
     * actual:
     * expect:
     */
    @Test
    public void encodeMultiTlv() {
        Encoder encoder = new Encoder();

        // [pos=3 lim=10 cap=10]
        // [0, 0, 0, 1, 0, 0, 2, 0, -4, 5]
        byte[] value1 = new byte[]{1, 0, 0, 2, 0};
        TLV TLV1 = new TLV(252, ByteBuffer.wrap(value1));
        encoder.prependTlv(TLV1);

        // [pos=3 lim=10 cap=10], remaining: 7
        System.out.println("buffer array: " + encoder.getWriteBuf() + ", remaining: " + encoder.getSize());
        // [0, 0, 0, -4, 5, 1, 0, 0, 2, 0]
        System.out.println("buffer str: " + Arrays.toString(encoder.getWriteBuf().array()));
        Assert.assertTrue("[-4, 5, 1, 0, 0, 2, 0]".equals(Strings.getRemainBytesString(encoder.getWriteBuf())));

        // 每次都是一个新的Encoder对象
        byte[] value2 = new byte[]{3, 4};
        TLV TLV2 = new TLV(8, ByteBuffer.wrap(value2));
        encoder.prependTlv(TLV2);

        // [pos=6 lim=10 cap=10], remaining: 4
        System.out.println("buffer array: " + encoder.getWriteBuf() + ", remaining: " + encoder.getSize());
        // [0, 0, 0, 0, 0, 0, 8, 2, 3, 4]
        System.out.println("buffer str: " + Arrays.toString(encoder.getWriteBuf().array()));

        Assert.assertTrue("[8, 2, 3, 4, -4, 5, 1, 0, 0, 2, 0]".equals(Strings.getRemainBytesString(encoder.getWriteBuf())));

        // [8, 2, 3, 4]
        System.out.println("slice: " + encoder.getWriteBuf() + ", slice: " + Strings.getRemainBytesString(encoder.getWriteBuf()));
    }


    @Test
    public void prependNestedTlv() {

        // 输入 T1-L1-[t2-l2-[t3-l3-v3]]
        // 输出 [12, 13, | -3, 0, -3, 9, | -3, -1, -1, 5, 11, 22, 33, 44, 55]

        Encoder encoder = new Encoder(10);
        // 12
        TLV tlv1 = new TLV(12, new byte[]{12, 2});

        // -3 0
        TLV tlv2 = new TLV(253, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        // -3 0
        TLV tlv3 = new TLV(65535, new byte[]{11, 22, 33, 44, 55});

        tlv2.addChildTLV(tlv3);
        tlv1.addChildTLV(tlv2);

        // tlv3 size : [t3-l3-v3] -> [3 - 1 - 5]
        Assert.assertTrue(String.format("tlv3 Size should be 9 - current %s", tlv3), tlv3.getTlvSize() == 9);
        Assert.assertTrue(String.format("tlv3 length should be 5 - current %s", tlv3), tlv3.getLength() == 5);

        // tlv2: t2-l2-[t3-l3-v3] ->  3 - 1 - [3 - 1 - 5]
        Assert.assertTrue(String.format("tlv2 Size should be 13 - current %s", tlv2), tlv2.getTlvSize() == 13);
        Assert.assertTrue(String.format("tlv2 length should be 9 - current %s", tlv2), tlv2.getLength() == 9);

        encoder.prependTlv(tlv1);

        // [pos=8 lim=23 cap=23]
        System.out.println("buffer array: " + encoder.getWriteBuf());

        // [0, 0, 0, 0, 0, 0, 0, 0, 12, 13, | -3, 0, -3, 9,  | -3, -1, -1, 5, 11, 22, 33, 44, 55]
        // [0, 0, 0, 0, 0, 0, 0, 0, 12, 13, -3, 0, -3, 9, -3, -1, -1, 5, 11, 22, 33, 44, 55]
        System.out.println("buffer str: " + Arrays.toString(encoder.getWriteBuf().array()));

        // 13 -3, -1, -1, 5, 11, 22, 33, 44, 55
        Assert.assertTrue("[12, 13, -3, 0, -3, 9, -3, -1, -1, 5, 11, 22, 33, 44, 55]".equals(Strings.getRemainBytesString(encoder.getWriteBuf())));
    }

}
