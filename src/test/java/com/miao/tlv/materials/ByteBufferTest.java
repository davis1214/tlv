package com.miao.tlv.materials;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ByteBufferTest {
    ByteOrder order = ByteOrder.LITTLE_ENDIAN;

    /**
     * byte:1个字节 8位 -128~127
     * <p>
     * short ：2个字节 16位
     * <p>
     * int ：4个字节 32位
     * <p>
     * long：8个字节 64位
     */
    @Test
    public void testByteBuffer() {
        ByteOrder order = ByteOrder.LITTLE_ENDIAN;

        // long
        long l = 2147483648L;
        byte[] bytes = ByteBuffer.allocate(8).order(order).putLong(l).array();
        long data1 = ByteBuffer.wrap(bytes, 0, bytes.length).order(order).getLong();
        System.out.println("data : " + data1);

        // int
        int i = 2;
        bytes = ByteBuffer.allocate(4).order(order).putInt(i).array();
        int data = ByteBuffer.wrap(bytes, 0, bytes.length).order(order).getInt();
//        System.out.println("data : " + data);
        System.out.println("data : " + data + ", " + Integer.toHexString((int) data));

        i = 256;
        bytes = ByteBuffer.allocate(8).order(order).putInt(i).putInt(0xFA).array();
        data = ByteBuffer.wrap(bytes, 0, bytes.length).order(order).getInt();
//        System.out.println("data : " + data);
//        System.out.println("data : " + data + ", " + Integer.toHexString((int) data));
        System.out.println("data : " + data);
        System.out.println("tag: " + ByteBuffer.wrap(bytes, 0, bytes.length - 4 * 1).getInt());

        i = 65535;
        bytes = ByteBuffer.allocate(12).putInt(0xFB).putInt(i).array();
        data = ByteBuffer.wrap(bytes, 0, bytes.length).getInt();
//        System.out.println("data : " + data);
        System.out.println("data : " + data + ", " + Integer.toHexString((int) data));


        i = 65535;
        bytes = ByteBuffer.allocate(4).putInt(i).array();
        data = ByteBuffer.wrap(bytes, 0, bytes.length).getInt();
        System.out.println("data : " + data + ", " + Integer.toHexString((int) data));

//        // short
//        short s = 32767;
//        bytes = ByteBuffer.allocate(2).order(order).putShort(s).array();
//        data = ByteBuffer.wrap(bytes, 0, bytes.length).order(order).getShort();
//        System.out.println("data : " + data);
    }


    @Test
    public void testWriteAndRead() {
        int s = 32767;
        byte[] bytes = ByteBuffer.allocate(4).order(order).putInt(s).array();
        int data = ByteBuffer.wrap(bytes, 0, bytes.length).order(order).getInt();
        System.out.println("data : " + data);
    }


    @Test
    public void testConvert() {
        System.out.println((126 & 0XFF) + " , " + (byte) (126 & 0XFF));
        System.out.println((252 & 0XFF) + " , " + (byte) (252 & 0XFF));
        System.out.println("->" + (250 & 0XFF) + " , " + (byte) (250 & 0XFF));
        System.out.println("->" + (1 & 0XFF) + " , " + (byte) (1 & 0XFF));
        System.out.println(((252 & 0XFF) & 0XFF) + " , " + (byte) (252 & 0XFF));
        System.out.println(((255 & 0XFF) & 0XFF));
    }

    @Test
    public void testBuffer() {
        // 初始化一个大小为6的ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
        // position: 0, limit: 3, capacity: 3, string: [0, 0, 0]
        print(buffer);

        // 往buffer中写入3个字节的数据
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);
        // position: 3, limit: 3, capacity: 3, string: [1, 2, 3]
        print(buffer);


        // duplicate
//        ByteBuffer duplicateBuffer = buffer.duplicate();

        System.out.println("************** after flip **************");
        buffer.flip();
        // position: 0, limit: 3, capacity: 3, string: [1, 2, 3]
        print(buffer);  // 切换为读取模式之后的状态：position: 0, limit: 3, capacity: 3
        //   print(duplicateBuffer);

        buffer.get();
        buffer.get();
        // position: 2, limit: 3, capacity: 3, string: [1, 2, 3]
        print(buffer);  // 读取两个数据之后的状态：position: 2, limit: 3, capacity: 3
    }

    @Test
    public void testExpandByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.position(3 - 2);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        print(buffer);  // 初始状态：position: 0, limit: 6, capacity: 6

        ByteBuffer expandBuffer = ByteBuffer.allocate(4 + buffer.capacity());
        int position = buffer.position();
        buffer.flip();

        expandBuffer.position(expandBuffer.limit() - position);
        //expandBuffer.position(position);
        expandBuffer.put(buffer);
        //expandBuffer.position(position);
        print(expandBuffer);

    }


    private void print(ByteBuffer buffer) {
        System.out.printf("position: %d, limit: %d, capacity: %d, string: %s\n",
                buffer.position(), buffer.limit(), buffer.capacity(), Arrays.toString(buffer.array()));
    }

}
