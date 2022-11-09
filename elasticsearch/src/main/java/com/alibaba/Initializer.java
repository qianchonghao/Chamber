package com.alibaba;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class Initializer implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("aa");
    }
}
