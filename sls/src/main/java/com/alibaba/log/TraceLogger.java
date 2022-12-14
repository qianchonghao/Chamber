package com.alibaba.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import static com.alibaba.log.Scene.*;
import static com.alibaba.log.Node.*;

/**
 * @author qch
 * @since 2022/12/14 5:16 下午
 */
@Slf4j
public class TraceLogger {
    private static Logger traceLogger;
    private static final String LOGGER_NAME = "trace_logger";
    private static final String FILE_PATTERN = "%d{yyyy-MM-dd}.gz";
    private static final String FILE_PATH = "/trace-logs/trace.log";
    private static final String PATTERN = "[%X{scene}] %d{yyyy-MM-dd HH:mm:ss}-[%F]-[line:%L] - %msg%n";

    static {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(LOGGER_NAME), "log name can't be empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(FILE_PATTERN), "log file can't be empty!");
        Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
        String filePath = System.getProperty("user.home") + FILE_PATH;

        try {
            Class.forName("ch.qos.logback.classic.Logger");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger tLogger = (ch.qos.logback.classic.Logger)logger;

                // init appender
                RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
                appender.setName(tLogger.getName());
                appender.setContext(tLogger.getLoggerContext());
                appender.setFile(filePath);
                appender.setAppend(true);

                // init policy
                SizeAndTimeBasedRollingPolicy policy = new SizeAndTimeBasedRollingPolicy();
                policy.setParent(appender);
                policy.setFileNamePattern(filePath + FILE_PATTERN);
                policy.setMaxHistory(1);
                policy.setMaxFileSize(new FileSize(100*FileSize.MB_COEFFICIENT));
                policy.setTotalSizeCap(new FileSize(20 * FileSize.GB_COEFFICIENT));
                policy.setContext(tLogger.getLoggerContext());
                policy.start();

                // init encoder
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setPattern(PATTERN);
                encoder.setCharset(Charset.forName(StandardCharsets.UTF_8.name()));
                encoder.setContext(tLogger.getLoggerContext());
                encoder.start();

                // start appender
                appender.setRollingPolicy(policy);
                appender.setEncoder(encoder);
//                registerCustomFilters(appender, filters);
                appender.start();

                // start logger
                tLogger.detachAndStopAllAppenders();
                tLogger.setAdditive(false);
                tLogger.setLevel(Level.ALL);
                tLogger.addAppender(appender);
//                ScriptExecutor.register(tLogger.getName(), appender);
            } else {
                log.warn(
                        "app [{}] not used Logback impl for Log, please set '{}' logger in your logger context manual.",
                        System.getProperty("project.name", "unknown"),
                        LOGGER_NAME);
            }
        } catch (ClassNotFoundException e) {
            log.warn("app [{}] not used Logback impl for Log, please set '{}' logger in your logger context manual.",
                    System.getProperty("project.name", "unknown"),
                    LOGGER_NAME, e);
        }

        traceLogger = logger;
    }
    private static final String ACID = "acid";
    /**
     * 规范场景的输出
     *      意味着业务场景全部得提供acid？
     *      业务链路的起点or中断点，需要设置MDC
     * MDC content：node维度
     *      1. scene 场景： 如外呼、入呼
     *      2. node 节点： 如入呼场景中 【processInvite, 建立userRTP通道， 建立ivrRTP通道】
     *      3. bizId 业务id： 场景和业务ID类型 一一映射关系， 如入呼场景 -> acid， 在线场景 -> touchId
     *          按照 scene + bizId 唯一检索出 整条链路的所有节点
     *      4.
     * 实现 node 数据记录：
     *      以下是配置态内容，运行态需要获取
     *      1. annotation： method级别注解（但是没有统一收敛点，且排序不便）
     *      2. json： 上传oss，然后初始化时，下拉解析. 但是无法关联 node -> method
     *      3. enum 目前采用3
     *
     */

    private static String SCENE = "scene";
    private static String NODE = "node";
    private static String BIZ_ID = "bizId"
    public static void refreshMDC(Node node, String bizId){
        Preconditions.checkArgument(node.isRefreshPoint(),"当前node不可刷新MDC，node = "+node.name());
        MDC.remove(SCENE);
        MDC.put()
    }
    public static void info(Node node, String format, Object... params){
        node.getScene().name();
        node.name();
        node.isRefreshPoint();

        traceLogger.info(format,params);
    }

    public static void warnWithAcid(String format,String acid,Object... params){
        traceLogger.warn(format,params);
    }

    public static void errorWithAcid(String format,String acid,Object... params){
        traceLogger.error(format,params);
    }


    private static Logger getLogger(){
        return traceLogger;
    }


    //    private static void registerCustomFilters(RollingFileAppender<ILoggingEvent> appender, List<TlogFilter> filters) {
//        if (filters == null || filters.isEmpty()) {
//            return;
//        }
//
//        for (TlogFilter filter : filters) {
//            appender.addFilter(new Filter<ILoggingEvent>() {
//                @Override
//                public FilterReply decide(ILoggingEvent event) {
//                    TLogContext context = new TLogContext(event, event.getFormattedMessage());
//                    TlogFilter.FilterReply reply = filter.decide(context);
//                    if (reply == null) {
//                        return DENY;
//                    } else {
//                        return FilterReply.valueOf(reply.name());
//                    }
//                }
//            });
//        }
//    }
}
