package com.alibaba.util;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * @author qch
 * @since 2022/12/19 9:38 下午
 */
@Slf4j
public class SqlUtils {
    public static boolean executeSql(DataSource dataSource, String sql, Object... params) {
        try {
            PreparedStatement ps = dataSource.getConnection().prepareStatement(sql);

            return ps.execute();
        } catch (SQLException e) {
            log.error("sql execute fail", e);
        }
        return false;
    }

    public static <T> T executeQuery(DataSource dataSource, String sql, Function<ResultSet, T> func, Object... params) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            rs = ps.executeQuery();
            return func.apply(rs);

        } catch (SQLException e) {
            log.error("sql execute fail", e);
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                log.error("datasource close fail", e);
            }
        }
    }

    public static int executeUpdate(DataSource dataSource, String sql, Object... params) {
        Connection connection = null;
        PreparedStatement ps = null;
        int res = 0;

        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            res = ps.executeUpdate();
        } catch (SQLException e) {
            log.error("sql execute fail", e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
                if (ps != null) {
                    ps.close();
                }

            } catch (SQLException e) {
                log.error("datasource close fail", e);
            }
        }

        return res;
    }

}
