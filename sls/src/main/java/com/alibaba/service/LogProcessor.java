package com.alibaba.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.service.model.LogInfo;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.loghub.client.ILogHubCheckPointTracker;
import com.aliyun.openservices.loghub.client.exceptions.LogHubCheckPointException;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessor;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;

import java.util.List;


/**
 * @author qch
 * @since 2022/12/14 11:29 上午
 * 自定义业务链路，追踪日志中的业务节点
 */
@Slf4j
public class LogProcessor implements ILogHubProcessor {

    private int shardId;

    // 记录上次持久化Checkpoint的时间。
    private long mLastCheckTime = 0;

    private DataSource dataSource;

    public LogProcessor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void initialize(int shardId) {
        this.shardId = shardId;
    }

    // 消费数据的主逻辑，消费时的所有异常都需要处理，不能直接抛出。
    @Override
    public String process(List<LogGroupData> logGroups, ILogHubCheckPointTracker checkPointTracker) {
        // 打印已获取的数据。
        for (LogGroupData logGroup : logGroups) {
            FastLogGroup flg = logGroup.GetFastLogGroup();

            // 1. 获取logtail信息
//            System.out.println(String.format("\tcategory\t:\t%s\n\tsource\t:\t%s\n\ttopic\t:\t%s\n\tmachineUUID\t:\t%s",
//                    flg.getCategory(), flg.getSource(), flg.getTopic(), flg.getMachineUUID()));
//            System.out.println("Tags");

            // 2. 获取 sls日志的tag
//            for (int tagIdx = 0; tagIdx < flg.getLogTagsCount(); ++tagIdx) {
//                FastLogTag logtag = flg.getLogTags(tagIdx);
//                System.out.println(String.format("\t%s\t:\t%s", logtag.getKey(), logtag.getValue()));
//            }
            // 3. 获取sls日志的content
            for (int lIdx = 0; lIdx < flg.getLogsCount(); ++lIdx) {
                FastLog log = flg.getLogs(lIdx);
                System.out.println("--------\nLog: " + lIdx + ", time: " + log.getTime() + ", GetContentCount: " + log.getContentsCount());
                for (int cIdx = 0; cIdx < log.getContentsCount(); ++cIdx) {
                    FastLogContent content = log.getContents(cIdx);
                    LogInfo info = parseContent(content);
                    // @leimo todo : 获取到消息对象后，执行写入数据库操作，注意重复create操作 抛错仅打印error日志
                    // @leimo todo ： 【later】记录消费位点，目前sls控制台可以重置消费位点，log数据丢失影响不大。暂时不提供
                }
            }
        }

        saveCheckPointIfNeed(checkPointTracker);
        return null;
    }

    private static final String START_MARK = "[";
    private static final String END_MARK = "]";
    private static final String DOT = ",";
    private static final String SPLIT = ":";

    private LogInfo parseContent(FastLogContent content) {
        String contentValue = content.getValue();
        int start = contentValue.indexOf(START_MARK);
        int end = contentValue.indexOf(END_MARK);
        contentValue = contentValue.substring(start + 1, end);
        JSONObject contentJSON = new JSONObject();

        Lists.newArrayList(contentValue.split(DOT)).stream().forEach(
                kv -> {
                    String[] kvs = kv.split(SPLIT);
                    if (kvs != null && kvs.length == 2) {
                        contentJSON.put(kvs[0].trim(), kvs[1].trim());
                    }
                }
        );
        contentJSON.put("content", content.getValue());
        LogInfo logInfo = contentJSON.toJavaObject(LogInfo.class);
        logInfo.insert(dataSource);
        return logInfo;
    }

    private void saveCheckPointIfNeed(ILogHubCheckPointTracker checkPointTracker) {
        long curTime = System.currentTimeMillis();
        // 每隔30秒，写一次Checkpoint到服务端。如果30秒内发生Worker异常终止，新启动的Worker会从上一个Checkpoint获取消费数据，可能存在少量的重复数据。
        if (curTime - mLastCheckTime > 30 * 1000) {
            try {
                //参数为true表示立即将Checkpoint更新到服务端；false表示将Checkpoint缓存在本地，默认间隔60秒会将Checkpoint更新到服务端。
                checkPointTracker.saveCheckPoint(true);
            } catch (LogHubCheckPointException e) {
                log.error("save checkPoint fail",e);
            }
            mLastCheckTime = curTime;
        }
    }

    @Override
    public void shutdown(ILogHubCheckPointTracker checkPointTracker) {
        try {
            checkPointTracker.saveCheckPoint(true);
        } catch (LogHubCheckPointException e) {
            log.error("save checkpoint fail",e);
        }
    }
}
