package edu.university.academic.test;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBTest {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/acadamiccompetition?useSSL=false&serverTimezone=UTC",
                "root",
                "ltssilqcq"
        );
        System.out.println("MySQL 连接成功");
        conn.close();
    }
}
