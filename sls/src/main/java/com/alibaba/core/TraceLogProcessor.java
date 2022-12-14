package com.alibaba.core;

import com.alibaba.log.TraceLogger;
import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.loghub.client.ILogHubCheckPointTracker;
import com.aliyun.openservices.loghub.client.exceptions.LogHubCheckPointException;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author qch
 * @since 2022/12/14 11:29 上午
 * 自定义业务链路，追踪日志中的业务节点
 */
@Slf4j
public class TraceLogProcessor implements ILogHubProcessor {
    private int shardId;

    // 记录上次持久化Checkpoint的时间。
    private long mLastCheckTime = 0;

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

            System.out.println(String.format("\tcategory\t:\t%s\n\tsource\t:\t%s\n\ttopic\t:\t%s\n\tmachineUUID\t:\t%s",
                    flg.getCategory(), flg.getSource(), flg.getTopic(), flg.getMachineUUID()));
            System.out.println("Tags");

            for (int tagIdx = 0; tagIdx < flg.getLogTagsCount(); ++tagIdx) {
                FastLogTag logtag = flg.getLogTags(tagIdx);
                System.out.println(String.format("\t%s\t:\t%s", logtag.getKey(), logtag.getValue()));
            }
            for (int lIdx = 0; lIdx < flg.getLogsCount(); ++lIdx) {
                FastLog log = flg.getLogs(lIdx);
                System.out.println("--------\nLog: " + lIdx + ", time: " + log.getTime() + ", GetContentCount: " + log.getContentsCount());
                for (int cIdx = 0; cIdx < log.getContentsCount(); ++cIdx) {
                    FastLogContent content = log.getContents(cIdx);
                    System.out.println(content.getKey() + "\t:\t" + content.getValue());
//                    TraceLogger.getLogger().info(content.getKey() + "\t:\t" + content.getValue());
                }
            }
        }
        long curTime = System.currentTimeMillis();
        // 每隔30秒，写一次Checkpoint到服务端。如果30秒内发生Worker异常终止，新启动的Worker会从上一个Checkpoint获取消费数据，可能存在少量的重复数据。
        if (curTime - mLastCheckTime > 30 * 1000) {
            try {
                //参数为true表示立即将Checkpoint更新到服务端；false表示将Checkpoint缓存在本地，默认间隔60秒会将Checkpoint更新到服务端。
                checkPointTracker.saveCheckPoint(true);
            } catch (LogHubCheckPointException e) {
                e.printStackTrace();
            }
            mLastCheckTime = curTime;
        }
        return null;
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
