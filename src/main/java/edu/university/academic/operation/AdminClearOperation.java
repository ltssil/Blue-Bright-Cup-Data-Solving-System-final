package edu.university.academic.operation;

import edu.university.academic.utils.DBUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;

public class AdminClearOperation {

    /**
     * 允许被清空的表（白名单）
     */
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(
            Arrays.asList(
                    "students",
                    "teachers",
                    "sign",
                    "award",
                    "finalreport",
                    "conflict"
            )
    );

    /**
     * 执行清空操作
     */
    public static void clearTables(List<String> tables) {

        if (tables == null || tables.isEmpty()) {
            throw new RuntimeException("未选择任何需要清空的表");
        }

        for (String table : tables) {
            if (!ALLOWED_TABLES.contains(table)) {
                throw new RuntimeException("非法表名：" + table);
            }
        }

        // 关闭外键检查（防止 sign / award 依赖问题）

        try {
            Class.forName(DBUtil.DRIVER) ;
            try(Connection conn = DriverManager.getConnection(DBUtil.URL, DBUtil.USER, DBUtil.PASS);) {

                String sql = "SET FOREIGN_KEY_CHECKS = 0";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.executeUpdate();

                for(String table : tables){
                    ps = conn.prepareStatement("TRUNCATE TABLE " + table);
                    ps.executeUpdate();
                }

                sql = "SET FOREIGN_KEY_CHECKS = 1";
                ps = conn.prepareStatement(sql);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
