package com.alibaba;

import com.alibaba.core.TraceLogProcessFactory;
import com.aliyun.openservices.loghub.client.ClientWorker;
import com.aliyun.openservices.loghub.client.config.LogHubConfig;
import com.aliyun.openservices.loghub.client.exceptions.LogHubClientWorkerException;
import org.springframework.stereotype.Component;

/**
 * @author qch
 * @since 2022/12/14 11:00 上午
 */
@Component
public class LogConsumeStarter {


    // 日志服务域名，请您根据实际情况填写。更多信息，请参见服务入口。
    private static String sEndpoint = "cn-hangzhou.log.aliyuncs.com";
    // 日志服务项目名称，请您根据实际情况填写。请从已创建项目中获取项目名称。
    private static String sProject = "k8s-hangzhou-pre";
    // 日志库名称，请您根据实际情况填写。请从已创建日志库中获取日志库名称。
    private static String sLogstore = "xixi-case";
    // 消费组名称，请您根据实际情况填写。您无需提前创建，该程序运行时会自动创建该消费组。
    private static String sConsumerGroup = "xixi-case-consumer";
    // 消费数据的用户AccessKey ID和AccessKey Secret信息，请您根据实际情况填写。更多信息，请参见访问密钥。

    // @leimo todo: 请勿上传

    private static String sAccessKeyId = "ak";
    private static String sAccessKey = "sk";
    public void start(){
        try {
            // consumer_1是消费者名称，同一个消费组下面的消费者名称必须不同，不同的消费者名称在多台机器上启动多个进程，来均衡消费一个Logstore，此时消费者名称可以使用机器IP地址来区分。
            // maxFetchLogGroupSize用于设置每次从服务端获取的LogGroup最大数目，使用默认值即可。您可以使用config.setMaxFetchLogGroupSize(100);调整，请注意取值范围为(0,1000]。
            LogHubConfig config = new LogHubConfig(sConsumerGroup, "consumer_1", sEndpoint, sProject, sLogstore, sAccessKeyId, sAccessKey, LogHubConfig.ConsumePosition.BEGIN_CURSOR, 1000);
            ClientWorker worker = new ClientWorker(new TraceLogProcessFactory(), config);
            Thread thread = new Thread(worker);
            //Thread运行之后，ClientWorker会自动运行，ClientWorker扩展了Runnable接口。
            thread.start();
            Thread.sleep(60 * 60 * 1000);
            //调用Worker的Shutdown函数，退出消费实例，关联的线程也会自动停止。
            worker.shutdown();
            //ClientWorker运行过程中会生成多个异步的任务，Shutdown完成后请等待还在执行的任务安全退出，建议sleep配置为30秒。
            Thread.sleep(30 * 1000);
        } catch (LogHubClientWorkerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
