package com.alibaba.service;

import com.alibaba.config.DataSourceConfiguration;
import com.alibaba.config.SLSLogConfig;
import com.alibaba.log.model.Node;
import com.alibaba.log.model.Scene;
import com.alibaba.util.SqlUtils;
import com.aliyun.openservices.loghub.client.ClientWorker;
import com.aliyun.openservices.loghub.client.config.LogHubConfig;
import com.aliyun.openservices.loghub.client.exceptions.LogHubClientWorkerException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.alibaba.util.IPUtils.getIP;

/**
 * @author qch
 * @since 2022/12/14 11:00 上午
 */
@Component
@Slf4j
public class LogConsumeStarter implements InitializingBean {
    @Autowired
    private SLSLogConfig SLSLogConfig0;

    @Autowired
    private DataSourceConfiguration dataSourceConfiguration;

//    // 日志服务域名，请您根据实际情况填写。更多信息，请参见服务入口。
//    private static String sEndpoint = "cn-hangzhou.log.aliyuncs.com";
//    // 日志服务项目名称，请您根据实际情况填写。请从已创建项目中获取项目名称。
//    private static String sProject = "k8s-hangzhou-pre";
//    // 日志库名称，请您根据实际情况填写。请从已创建日志库中获取日志库名称。
//    private static String sLogstore = "xixi-case";
//    // 消费组名称，请您根据实际情况填写。您无需提前创建，该程序运行时会自动创建该消费组。
//    private static String sConsumerGroup = "xixi-case-consumer";
//    // 消费数据的用户AccessKey ID和AccessKey Secret信息，请您根据实际情况填写。更多信息，请参见访问密钥。
    // @leimo todo: 请勿上传
//    private static String sAccessKeyId = "ak";
//    private static String sAccessKey = "sk";
    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("SLS_logger_consumer_%d").build();
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>());
    public void init(){
        try {
            // consumer_1是消费者名称，同一个消费组下面的消费者名称必须不同，不同的消费者名称在多台机器上启动多个进程，来均衡消费一个Logstore，此时消费者名称可以使用机器IP地址来区分。
            // maxFetchLogGroupSize用于设置每次从服务端获取的LogGroup最大数目，使用默认值即可。您可以使用config.setMaxFetchLogGroupSize(100);调整，请注意取值范围为(0,1000]。
//            @leimo todo: 需要获取 redis 分布式锁，xixi-mcu应用只有一台pod在执行消费。
            String consumer = getIP();
            LogHubConfig config = new LogHubConfig(SLSLogConfig0.getConsumerGroup(), consumer, SLSLogConfig0.getEndPoint(), SLSLogConfig0.getProject(), SLSLogConfig0.getLogStore(), SLSLogConfig0.getAccessKey(), SLSLogConfig0.getSecretKet(), LogHubConfig.ConsumePosition.BEGIN_CURSOR, 1000);
            // 执行process的间隔时间，默认200ms
            config.setFetchIntervalMillis(1000);
            ClientWorker worker = new ClientWorker(new LogProcessFactory(dataSourceConfiguration.getDataSource()), config);
            // @leimo ClientWorker运行过程中会生成多个异步的任务（取决于logtail的分片数量，每个分片有一个对应的process）
            // Shutdown完成后请等待还在执行的任务安全退出，建议sleep配置为30秒。
            executor.execute(worker);
            //Thread运行之后，ClientWorker会自动运行，ClientWorker扩展了Runnable接口。
//            thread.start();
//            Thread.sleep(60 * 60 * 1000);
//            //调用Worker的Shutdown函数，退出消费实例，关联的线程也会自动停止。
//            worker.shutdown();
//            //ClientWorker运行过程中会生成多个异步的任务，Shutdown完成后请等待还在执行的任务安全退出，建议sleep配置为30秒。
//            Thread.sleep(30 * 1000);
        } catch (LogHubClientWorkerException e) {
            log.error("sls log consumer start fail",e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
