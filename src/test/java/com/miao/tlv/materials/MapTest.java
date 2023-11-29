package com.miao.tlv.materials;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

@Slf4j
public class MapTest {

    @Test
    public void testMapGetOrDefault() {

        Map<String, Map<String, Number>> map = Maps.newHashMap();
        String key1 = "key1";

        map.getOrDefault(key1, Maps.newHashMap()).put("12", 12);
        map.getOrDefault(key1, Maps.newHashMap()).put("13", 13);

        Assert.assertTrue(map.size() == 0);
        Assert.assertTrue(!map.containsKey(key1));


        map.computeIfAbsent(key1, key -> Maps.newHashMap()).put("12", 12);
        map.computeIfAbsent(key1, key -> Maps.newHashMap()).put("13", 13);
        Assert.assertTrue(map.size() == 1);
        Assert.assertTrue(map.containsKey(key1));
        Assert.assertTrue(map.get(key1).size() == 2);
    }

}
