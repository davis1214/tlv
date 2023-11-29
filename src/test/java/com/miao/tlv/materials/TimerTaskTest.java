package com.miao.tlv.materials;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimerTaskTest {

    private TimerTask timerTask;

    private CountDownLatch countDownLatch;

    @Before
    public void before() {
        timerTask = new TimerTask() {
            private String taskName = "timer task";

            @Override
            public void run() {
                log.info("任务「" + taskName + "」被执行 - {}", new Date());
                countDownLatch.countDown();
            }
        };

        countDownLatch = new CountDownLatch(1);
    }


    @Test
    public void testTimer() {
        Timer timer = new Timer();
        timer.schedule(timerTask, 5000L);

        log.info("submit - {}", new Date());
        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("stop - {}", new Date());
    }

}
