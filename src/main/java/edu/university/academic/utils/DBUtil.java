package edu.university.academic.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {
    public final static String DRIVER = "com.mysql.cj.jdbc.Driver";
    public final static String URL = "jdbc:mysql://localhost:3306/acadamiccompetition"
            + "?useSSL=false"
            + "&serverTimezone=Asia/Shanghai"
            + "&characterEncoding=utf8";
    public final static String USER = "ltssil";
    public final static String PASS = "ltssilqcq";
    private static final HikariDataSource DATA_SOURCE;

    static {
        try {
            Class.forName(DRIVER);
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASS);
            config.setDriverClassName(DRIVER);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(60_000);
            config.setMaxLifetime(600_000);
            DATA_SOURCE = new HikariDataSource(config);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static int update(String sql, Object[] args) {
        int row = -1 ;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            for(int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }

            row = pstmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return row ;
    }

    public static <T> List<T> query(String sql, Object[] args , RowProcessor<T> rowProcessor) {
        List<T> res = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            for(int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    T row = rowProcessor.process(rs);
                    res.add(row);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return res ;
    }

    public static int[] executeBatchUpdate(String sql, List<Object[]> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return new int[0];
        }

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Object[] params : paramsList) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                    pstmt.addBatch();
                }
            }
            int[] results = pstmt.executeBatch();
            conn.commit();
            return results;
            }
        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            ex.printStackTrace();
            return new int[0];
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

}
