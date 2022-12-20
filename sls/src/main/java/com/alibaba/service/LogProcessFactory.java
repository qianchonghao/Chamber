package com.alibaba.service;

import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessor;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessorFactory;

import javax.sql.DataSource;

/**
 * @author qch
 * @since 2022/12/14 11:31 上午
 */
public class LogProcessFactory implements ILogHubProcessorFactory {
    private DataSource dataSource;

    public LogProcessFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ILogHubProcessor generatorProcessor() {
        return new LogProcessor(dataSource);
    }
}
