package com.hmdp.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RedisIdWorker {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 2024年0秒0分时间
     */
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    /**
     * 左移位数
     */
    private static final int COUNT_BITS = 32;

    public long nextId(String keyPrefix){
        //获取当前秒数
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond-BEGIN_TIMESTAMP;
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String icrKey = "icr:" + keyPrefix + ":" + currentDate;
        //获取自增长值
        Long count = stringRedisTemplate.opsForValue().increment(icrKey);

        return (timestamp << COUNT_BITS) | count;
    }

}
