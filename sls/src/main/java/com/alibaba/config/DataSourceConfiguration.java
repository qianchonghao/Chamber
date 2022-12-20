package com.alibaba.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.CharStreams;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

/**
 * @author qch
 * @since 2022/12/18 3:21 下午
 */
@Component
@Data
@Slf4j
public class DataSourceConfiguration implements InitializingBean {

    private DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        parseConfig();
    }

    private void parseConfig() {
        InputStream is = DataSourceConfiguration.class.getClassLoader().getResourceAsStream("./com.alibaba/datasource.json");
        String contentStr;

        try {
            contentStr = CharStreams.toString(new InputStreamReader(is));
            JSONObject config = JSONObject.parseObject(contentStr);

            DruidDataSource dataSource = new DruidDataSource();
            this.dataSource = DruidDataSourceFactory.createDataSource(config);

            dataSource.init();

            log.info("datasourceConfig = {}", config);
        } catch (Exception e) {
            log.error("datasource init fail", e);
        }

    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
