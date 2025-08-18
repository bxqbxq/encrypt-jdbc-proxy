package com.seu.jdbcproxy;

import com.seu.jdbcproxy.tester.ProxyDriver;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Properties;

/**
 * 异常处理测试类
 * 测试各种异常情况下的错误传递和处理
 */
public class ExceptionHandlingTest {

    @Test
    public void testConnectionNotFound() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            // 建立连接
            Connection conn = DriverManager.getConnection(url, props);
            
            // 关闭连接
            conn.close();
            
            // 尝试使用已关闭的连接执行查询
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                stmt.executeQuery();
            } catch (SQLException e) {
                System.out.println("Expected SQLException: " + e.getMessage());
                System.out.println("SQL State: " + e.getSQLState());
                System.out.println("Error Code: " + e.getErrorCode());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidSQL() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 执行无效的SQL语句
                try {
                    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM non_existent_table");
                    stmt.executeQuery();
                } catch (SQLException e) {
                    System.out.println("Expected SQLException for invalid table: " + e.getMessage());
                    System.out.println("SQL State: " + e.getSQLState());
                    System.out.println("Error Code: " + e.getErrorCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidParameters() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 测试参数类型不匹配
                try {
                    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM my_table WHERE id = ?");
                    stmt.setString(1, "invalid_id_type"); // 应该是int类型
                    stmt.executeQuery();
                } catch (SQLException e) {
                    System.out.println("Expected SQLException for parameter type mismatch: " + e.getMessage());
                    System.out.println("SQL State: " + e.getSQLState());
                    System.out.println("Error Code: " + e.getErrorCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResultSetNotFound() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 正常执行查询
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM my_table LIMIT 1");
                ResultSet rs = stmt.executeQuery();
                
                // 关闭ResultSet
                rs.close();
                
                // 尝试使用已关闭的ResultSet
                try {
                    rs.next();
                } catch (SQLException e) {
                    System.out.println("Expected SQLException for closed ResultSet: " + e.getMessage());
                    System.out.println("SQL State: " + e.getSQLState());
                    System.out.println("Error Code: " + e.getErrorCode());
                }
                
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStatementNotFound() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 创建Statement
                PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                
                // 关闭Statement
                stmt.close();
                
                // 尝试使用已关闭的Statement
                try {
                    stmt.executeQuery();
                } catch (SQLException e) {
                    System.out.println("Expected SQLException for closed Statement: " + e.getMessage());
                    System.out.println("SQL State: " + e.getSQLState());
                    System.out.println("Error Code: " + e.getErrorCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDatabaseConnectionError() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            // 使用无效的数据库连接信息
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://invalid_host:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try {
                Connection conn = DriverManager.getConnection(url, props);
            } catch (SQLException e) {
                System.out.println("Expected SQLException for invalid database: " + e.getMessage());
                System.out.println("SQL State: " + e.getSQLState());
                System.out.println("Error Code: " + e.getErrorCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTransactionRollback() {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 开始事务
                conn.setAutoCommit(false);
                
                try {
                    // 执行一些操作
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO my_table (test) VALUES (?)");
                    stmt.setString(1, "test_transaction");
                    stmt.executeUpdate();
                    
                    // 故意制造错误
                    stmt = conn.prepareStatement("INSERT INTO non_existent_table (col) VALUES (?)");
                    stmt.setString(1, "should_fail");
                    stmt.executeUpdate();
                    
                    // 如果到这里，提交事务
                    conn.commit();
                } catch (SQLException e) {
                    System.out.println("Transaction failed, rolling back: " + e.getMessage());
                    System.out.println("SQL State: " + e.getSQLState());
                    System.out.println("Error Code: " + e.getErrorCode());
                    
                    // 回滚事务
                    conn.rollback();
                    System.out.println("Transaction rolled back successfully");
                }
                
                // 恢复自动提交
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
