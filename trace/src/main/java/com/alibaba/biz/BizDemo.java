package com.alibaba.biz;

import com.alibaba.core.TraceConfiguration;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author qch
 * @since 2022/12/6 5:02 下午
 */
@Component
@Slf4j
public class BizDemo {
    @Autowired
    private TraceConfiguration traceConfiguration;


    public void execute(){
//        // 1. 父 span
//        Span parent = tracer.activeSpan();
//
//        // 2. 初始化span
//        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("first_biz").withTag("name","t1");
//        if (parent!=null) {
//            spanBuilder.asChildOf(parent);
//        }
//        Span childSpan = spanBuilder.start();
//
//        // 3. 激活span，执行请求
//
//        tracer.scopeManager().activate(childSpan,true);

        firstSpan();

    }

    // 创建根span
    private void firstSpan() {
        Tracer tracer = traceConfiguration.getTrace();

        // 1. 创建Span。
        Span span = tracer.buildSpan("parentSpan").withTag("myTag", "spanFirst").start();
        // 2. 激活span
        tracer.scopeManager().activate(span, false);
        // 3. 返回最近被激活的span
        tracer.activeSpan().setTag("methodName", "testTracing");
        // 业务逻辑。
        firstBiz();

        // 4. 结束span
        span.finish();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void firstBiz(){
        log.info("first biz");
        secondSpan();
    }

    // 创建span，span支持跟踪上下游链路
    private void secondSpan() {
        Tracer tracer = traceConfiguration.getTrace();

        // 1. 返回最近被激活额span
        Span parentspan = tracer.activeSpan();
        // 2. 构建childSpan
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("childSpan").withTag("myTag", "spanSecond");
        if (parentspan != null) {
            spanBuilder.asChildOf(parentspan).start();
        }
        Span childSpan = spanBuilder.start();

        // 3. 激活childSpan
        Scope scope = tracer.scopeManager().activate(childSpan,false); // 请求开始执行一次
            // 业务逻辑。 可以执行多次 buildSpan
        secondBiz();
        childSpan.finish();
        tracer.activeSpan().setTag("methodName", "testCall");

            // 请求结束执行一次
        scope.close();
    }



    private void secondBiz(){
        log.info("second biz");
    }

}
