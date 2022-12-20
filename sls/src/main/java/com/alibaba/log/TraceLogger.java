package com.alibaba.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.alibaba.log.model.Node;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * @author qch
 * @since 2022/12/14 5:16 下午
 */
@Slf4j
public class TraceLogger {
    private static Logger traceLogger;
    private static final String LOGGER_NAME = "trace_logger";
    private static final String FILE_PATH = "/trace-logs/trace_%d{yyyy-MM-dd}.log";
    // 定义 traceLog的pattern，根据 scene，node，bizId三级解析
    private static final String PATTERN = "[scene:%X{scene}, node:%X{node}, bizId:%X{bizId}] %d{yyyy-MM-dd HH:mm:ss}-[%F]-[line:%L] - %msg%n";
    static {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(LOGGER_NAME), "log name can't be empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(FILE_PATH), "file path can't be empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(PATTERN), " encoder pattern can't be empty!");

        Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

        try {
            Class.forName("ch.qos.logback.classic.Logger");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger tLogger = (ch.qos.logback.classic.Logger)logger;

                // init appender
                RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
                appender.setName(tLogger.getName());
                appender.setContext(tLogger.getLoggerContext());
                // 优先级高于 encoder.FileNamePattern,指定日志文件名
//                appender.setFile(filePath);
                appender.setAppend(true);

                // init policy
                // SizeAndTimeBasedPolicy 不能使用，原因不明
                TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
                policy.setParent(appender);
                String filePath = System.getProperty("user.home") + FILE_PATH;
                policy.setFileNamePattern(filePath);
                policy.setMaxHistory(1);
//                policy.setMaxFileSize(new FileSize(100*FileSize.MB_COEFFICIENT));
                policy.setTotalSizeCap(new FileSize(10 * FileSize.GB_COEFFICIENT));
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
    /**
     * MDC content：node维度
     *      1. scene 场景： 如外呼、入呼
     *      2. node 节点： 如入呼场景中 【processInvite, 建立userRTP通道， 建立ivrRTP通道】
     *      3. bizId 业务id： 场景和业务ID类型 一一映射关系， 如入呼场景 -> acid， 在线场景 -> touchId
     *          按照 scene + bizId 唯一检索出 整条链路的所有节点
     *
     * 描述 scene -> node的list结构：
     *      1. annotation： method级别注解（但是没有统一收敛点，且排序不便）
     *      2. json： 上传oss，然后初始化时，下拉解析. 但是无法关联 node -> method
     *      3. enum：代码描述 目前采用3
     *
     */

    private static String SCENE = "scene";
    private static String NODE = "node";
    private static String BIZ_ID = "bizId";
    // 供业务应用中 非refreshPoint的node使用
    public static String EMPTY_BIZ_ID = "";

    public static void refreshMDC(Node node, String bizId){
        MDC.put(NODE,node.name());

        if(node.isRefreshPoint() && StringUtils.isNotEmpty(bizId)) {
            MDC.put(SCENE,node.getScene().name());
            MDC.put(BIZ_ID,bizId);
        }
    }

    // node的信息必须更换，但是bizId和scene的信息不一定
    public static void info(Node node, String bizId, String format, Object... params) {
        refreshMDC(node, bizId);
        traceLogger.info(format, params);
    }

    public static void warn(Node node, String bizId, String format, Object... params) {
        refreshMDC(node, bizId);
        traceLogger.warn(format, params);
    }

    public static void error(Node node, String bizId, String format, Object... params) {
        refreshMDC(node, bizId);
        traceLogger.error(format, params);
    }


    public static Logger getLogger(){
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
