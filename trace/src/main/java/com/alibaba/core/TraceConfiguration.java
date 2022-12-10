package com.alibaba.core;

/**
 * @author qch
 * @since 2022/12/3 1:14 下午
 */

//import org.springframework.context.annotation.Configuration;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import io.jaegertracing.Configuration.*;
import io.jaegertracing.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;

/**
 * 1. 初始化全局Trace对象
 * 2.
 */
@Component
@Slf4j
public class TraceConfiguration implements InitializingBean {
    private TraceConfig config;

    private Tracer tracer;

    @Override
    public void afterPropertiesSet() {
        initTrace();
    }

    private boolean parseConfig(){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("./config/trace.json");

        try {
            String content = CharStreams.toString(new InputStreamReader(is));
            config = JSONObject.parseObject(content,TraceConfig.class);
            log.info("trace config = {}",content);
        } catch (IOException e) {
            log.error("parse trace config fail",e);
            return false;
        }
        return true;
    }

    private void initTrace(){
        Preconditions.checkArgument(parseConfig(),"trace config parse fail");

        // 1. set serviceName
        Configuration configuration = new Configuration(config.getServiceName());
        SenderConfiguration sender = new SenderConfiguration();
        // 2. 设置trace实例的endpoint
        sender.withEndpoint(config.getEndpoint());

        // trace(调用链路), span（逻辑节点），spanContext(shangxiawen1)
        configuration
                .withSampler(new SamplerConfiguration().withType("const").withParam(1))
                .withReporter(new ReporterConfiguration().withSender(sender).withMaxQueueSize(10000))
                .getTracerBuilder()
                // injector, extractor用于跨进程传递
                // inject: 实现trace编码spanContext，inject into carrier
                    // param = {format, Injector}
                // extractor 实现trace解码spanContext, extract from carrier
                    // param = {format, Extractor}
                .registerInjector(TEXT_MAP, TextMapCodec.builder()
                        .withBaggagePrefix(config.getBaggagePrefix())
                        .withSpanContextKey(config.getSpanContextKey())
                        .withUrlEncoding(true).build())
                .registerExtractor(TEXT_MAP, TextMapCodec.builder()
                        .withBaggagePrefix(config.getBaggagePrefix())
                        .withSpanContextKey(config.getSpanContextKey())
                        .withUrlEncoding(true).build())
                        .build();
        GlobalTracer.register(configuration.getTracer());
        tracer = GlobalTracer.get();
    }

    public Tracer getTrace(){
        return tracer;
    }

    @Data
    private static class TraceConfig{
        private String endpoint;
        private String serviceName;
        private String baggagePrefix = "baggage_prefix";
        private String spanContextKey = "context_key";
    }


}
