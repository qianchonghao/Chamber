package com.alibaba;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author qch
 * @since 2022/12/14 11:51 上午
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SlsLogConsumerTest {
    @Autowired
    private LogConsumeStarter starter;

    @Test
    public void start() {
        starter.start();

    }
}
