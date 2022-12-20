package com.alibaba.service.model;

import com.alibaba.util.SqlUtils;
import lombok.Data;

import javax.sql.DataSource;

/**
 * @author qch
 * @since 2022/12/19 3:07 下午
 */
@Data
public class LogInfo {
    private String scene;
    private String node;
    private String bizId;
    private String content;

    private static final String INSERT_SLS_LOG = "INSERT INTO `cco_xixi_hotline_session`.`sls_log_info`\n" +
            "(`scene`,`node`,`biz_id`,`content`) VALUES\n" +
            "(?,?,?,?)";

    public int insert(DataSource dataSource) {
        return SqlUtils.executeUpdate(dataSource, INSERT_SLS_LOG,
                this.getScene(), this.getNode(), this.getBizId(), this.getContent());
    }
}
