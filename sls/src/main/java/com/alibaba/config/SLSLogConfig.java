package com.alibaba.config;

import com.alibaba.fastjson.JSONObject;
import com.google.common.io.CharStreams;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author qch
 * @since 2022/12/18 3:21 下午
 */
@Component
@Data
@Slf4j
public class SLSLogConfig implements InitializingBean {
//    private Config config;

    private String endPoint;
    private String project;
    private String logStore;
    private String consumerGroup;
    private String accessKey;
    private String secretKet;

    @Override
    public void afterPropertiesSet() throws Exception {
        parseConfig();
    }

    private void parseConfig() {
        InputStream is = SLSLogConfig.class.getClassLoader().getResourceAsStream("./com.alibaba/config.json");
        String contentStr;

        try {
            contentStr = CharStreams.toString(new InputStreamReader(is));
            SLSLogConfig SLSLogConfig = JSONObject.parseObject(contentStr, SLSLogConfig.class);
            BeanUtils.copyProperties(SLSLogConfig,this);
        } catch (IOException e) {
            log.error("parse trace config fail",e);
        }
    }

}
