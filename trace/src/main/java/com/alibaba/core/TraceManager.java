package com.alibaba.core;

/**
 * @author qch
 * @since 2022/12/3 1:14 下午
 */


import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import io.jaegertracing.Configuration.*;
import io.jaegertracing.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;

/**
 * @author qch
 * @since 2022/12/12 10:03 上午
 * 1. Trace : 场景，如入呼、外呼 (所有场景共用 opentracing.tracer对象，但是span的tag带上统一traceOperationName)
 * 2. TraceManager: 管理trace，获取场景
 * 3. span： 逻辑节点
 *      3.1 如入呼场景中
 *          (1) 接受sip的Invite信令
 *          (2) 创建user的RTP通道
 *          (3) 创建ivr的RTP通道
 *          (4) link ivr RTP和 user的RTP，播放ivr
 *          (5) 用户按键事件 DTMF， 触发agent侧的webRTC通道建立
 *      // 以上逻辑表达方式两种： json or 子类implements方式实现
 *      // 不支持与或逻辑，先不做复杂，缺失节点则断定为error
 *      3.2 trace = stack of span, span需要 operationName,tag（tag标识为外呼、入呼？），
 *      3.3 根据json解析，创建多个 TraceRunner: tName -> sName ->
 */

@Component
@Slf4j
public class TraceManager implements InitializingBean {
    private TraceConfig config;

    private static Tracer tracer;

    @Override
    public void afterPropertiesSet() {
        initTrace();
    }

    private boolean parseConfig(){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("./config/trace.json");

        try {
            String content = CharStreams.toString(new InputStreamReader(is));
            config = JSONObject.parseObject(content, TraceConfig.class);
            List<TraceStructure> structures = config.getTraceStructures();
            structures.stream().forEach((traceStructure) -> {
                String tName = traceStructure.getTName();
                traceStructure.getSpans().stream().
                        forEach((spanStructure) -> {
                            String sName = spanStructure.getSName();
                            String key = buildKey(tName, sName);
                            TraceExecutor traceExecutor = new TraceExecutor(traceStructure,spanStructure);
                            traceFunctionMap.put(key, traceExecutor);
                        });
            });
            log.info("trace config = {}", content);
        } catch (IOException e) {
            log.error("parse trace config fail", e);
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

    public static Tracer getTrace(){
        return tracer;
    }

    private static String SEPARATOR = "-";
    Map<String, TraceExecutor> traceFunctionMap = Maps.newHashMap();

    public static String buildKey(String tName, String sName){
        return StringUtils.join(Lists.newArrayList(tName,sName),SEPARATOR);
    }

    // 让业务逻辑获取对应的 traceFunction，然后执行，上传到tracing。上传的内容包含 tname，sname，traceId，acid
    public TraceExecutor getTraceFunction(String tName, String sName){
        return traceFunctionMap.get(buildKey(tName,sName));
    }

//    public void test(){
//        TraceExecutor traceExecutor = getTraceFunction("tname","sname");
//        traceExecutor.runWithRootSpan((param)->{
//            System.out.println(param);
//            return param;
//        },"b");
//    }

    @Data
    public static class TraceConfig{
        private String endpoint;
        private String serviceName;
        private String baggagePrefix = "baggage_prefix";
        private String spanContextKey = "context_key";
        private List<TraceStructure> traceStructures;
    }

    @Data
    public static class TraceStructure {
        // trace structure
        private String tName;
        private List<SpanStructure> spans;
    }

    @Data
    public static class SpanStructure {
        private boolean isRoot = false;
        private String sName;
        private Map<String, String> tags;
    }
}
