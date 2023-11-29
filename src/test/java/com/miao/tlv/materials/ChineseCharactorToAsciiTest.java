package com.miao.tlv.materials;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class ChineseCharactorToAsciiTest {

    @Test
    public void testConvert() {
        String nick = "你好";
        byte[] bytes = nick.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            // -28/-67/-96/-27/-91/-67/
            System.out.print(bytes[i] + "/");
        }

        System.out.println("\n");
        for (int i = 0; i < nick.length(); i++) {
            char ch = nick.charAt(i);
            // 4f60, 597d
            String s4 = Integer.toHexString(ch);
            // + String.format("%x", s4)
            System.out.println(ch + "/" + s4 + "/");
        }
    }
}


