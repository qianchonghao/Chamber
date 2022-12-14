package com.alibaba.core;

import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessor;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessorFactory;

/**
 * @author qch
 * @since 2022/12/14 11:31 上午
 */
public class TraceLogProcessFactory implements ILogHubProcessorFactory {
    @Override
    public ILogHubProcessor generatorProcessor() {
        return new TraceLogProcessor();
    }
}
