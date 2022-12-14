package com.alibaba.core;

import io.opentracing.Span;
import io.opentracing.Tracer;


import com.alibaba.core.TraceManager.*;
import java.util.function.Function;

import static com.alibaba.model.TraceConst.SPAN_NAME;
import static com.alibaba.model.TraceConst.TRACE_NAME;

/**
 * @author qch
 * @since 2022/12/12 9:36 上午
 * trace-span level： execute task in span
 * 针对span纬度，执行逻辑。不同的调用实例，经过该span时，都走这段invoke逻辑
 */
public class TraceExecutor<T, R> {

    private TraceStructure traceStructure;
    private SpanStructure spanStructure;
    private Function<T, R> delegate;

    public TraceExecutor(TraceStructure traceStructure, SpanStructure spanStructure) {
        this.traceStructure = traceStructure;
        this.spanStructure = spanStructure;
    }

    public R runWithSpan(T param) {
        Tracer tracer = TraceManager.getTrace();

        // 1. 创建Span。
        Span span = tracer.buildSpan(spanStructure.getSName())
                .withTag(TRACE_NAME, traceStructure.getTName())
                .withTag(SPAN_NAME, spanStructure.getSName())
                .start();
        // 2. 激活span
        tracer.scopeManager().activate(span, false);
        // 3. 返回最近被激活的span
//        tracer.activeSpan().setTag("methodName", "testTracing");
        // 业务逻辑
        R result = delegate.apply(param);
        // 4. 结束span
        span.finish();
        return result;
    }

//    public R runWithSpan(T param) {
//        return spanStructure.isRoot() ? runWithRootSpan(param) : runWithChildSpan(param);
//    }

    // 创建根span
//    public R runWithRootSpan(T param) {
//        Tracer tracer = TraceManager.getTrace();
//
//        // 1. 创建Span。
//        Span span = tracer.buildSpan(spanStructure.getSName())
//                .withTag(TRACE_NAME, traceStructure.getTName())
//                .withTag(SPAN_NAME, spanStructure.getSName())
//                .start();
//        // 2. 激活span
//        tracer.scopeManager().activate(span, false);
//        // 3. 返回最近被激活的span
////        tracer.activeSpan().setTag("methodName", "testTracing");
//        // 业务逻辑
//        R result = delegate.apply(param);
//        // 4. 结束span
//        span.finish();
//        return result;
//    }
/**
 * trace.activeSpan() ： 本质threadLocal，返回当前线程中激活的前一个span，不适用异步
 *
 *
 */
//    // 创建span，span支持跟踪上下游链路
//    private R runWithChildSpan(Function<T, R> func, T t) {
//        Tracer tracer = TraceManager.getTrace();
//
//        // 1. 返回最近被激活额span
//        Span parentspan = tracer.activeSpan();
//        // 2. 构建childSpan
//        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("childSpan").withTag("myTag", "spanSecond");
//        if (parentspan != null) {
//            spanBuilder.asChildOf(parentspan).start();
//        }
//        Span childSpan = spanBuilder.start();
//
//        // 3. 激活childSpan
//        Scope scope = tracer.scopeManager().activate(childSpan,false); // 请求开始执行一次
//        // 业务逻辑。 可以执行多次 buildSpan
//
//        R result = func.apply(t);
//        childSpan.finish();
//        tracer.activeSpan().setTag("methodName", "testCall");
//
//        // 请求结束执行一次
//        scope.close();
//
//        return result;
//    }

}
