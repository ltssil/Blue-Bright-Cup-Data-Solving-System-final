package edu.university.academic.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    static {
        try {
            Class.forName(DRIVER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int update(String sql, Object[] args) {
        int row = -1 ;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return new int[0];
        }
    }

}
