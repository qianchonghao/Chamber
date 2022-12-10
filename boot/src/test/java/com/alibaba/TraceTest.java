package com.alibaba;

import com.alibaba.biz.BizDemo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
//import com.alibaba.ChamberApplication;
/**
 * @author qch
 * @since 2022/12/6 4:51 下午
 */
@RunWith(SpringRunner.class)  //底层用junit  SpringJUnit4ClassRunner

//@SpringBootTest(classes = ChamberApplication.class)//启动整个springboot工程
@SpringBootTest()//启动整个springboot工程
public class TraceTest {
    @Autowired
    private BizDemo bizDemo;
    @Test
    public void test(){

      bizDemo.execute();
 }
}
