package com.seu.jdbcproxy;

import com.seu.jdbcproxy.tester.ProxyDriver;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Properties;

public class SimpleTest {

    @Test
    public void simpleSelect() throws SQLException {
        try {
            // 注册 ProxyDriver
            DriverManager.registerDriver(new ProxyDriver());

            // 设置 JDBC URL（注意 realUrl 用 ? 拼接）
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";

            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {

                String sql = "select id,test from my_table";  //根据具体需求进行修改
                PreparedStatement preparedStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet rs = preparedStatement.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt(1);       //根据不同的查询字段进行修改
                    String test = rs.getString(2);
                    System.out.println("ID: " + id + "  Test: " + test);
                }

                // 显式关闭资源
                rs.close();
                preparedStatement.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInsert() throws SQLException {
        try {
            // 注册 ProxyDriver
            DriverManager.registerDriver(new ProxyDriver());

            // 设置 JDBC URL（注意 realUrl 用 ? 拼接）
            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";

            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {

                String sql = "insert into my_table (test) values (?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, "李四六");
                int rs = preparedStatement.executeUpdate();
                //System.out.println(rs);
                if (rs > 0) {
                    //logger.info("添加成功");
                    System.out.println("Successfully executed");
                } else {
                    System.out.println("Failed to execute");
                }
                
                preparedStatement.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdate() throws SQLException {
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                String sql = "update my_table set test = ? where id = ?";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, "update_value");
                preparedStatement.setInt(2, 26);
                int rs = preparedStatement.executeUpdate();
                if (rs > 0) {
                    System.out.println("Successfully executed");
                } else {
                    System.out.println("Failed to execute");
                }

                preparedStatement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDelete() throws SQLException {
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                String sql = "delete from my_table where id = ?";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setInt(1, 26);
                int rs = preparedStatement.executeUpdate();
                if (rs > 0) {
                    System.out.println("Successfully deleted");
                } else {
                    System.out.println("Failed to delete");
                }

                preparedStatement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleStatements() throws SQLException {  //有问题
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 创建多个Statement，测试并发安全性
                PreparedStatement stmt1 = conn.prepareStatement("select id,test from my_table",ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                //PreparedStatement stmt2 = conn.prepareStatement("select id,test from my_table");
                
                // 设置参数
                //stmt1.setString(1, "qwerqwer");
                
                // 执行查询
                ResultSet rs1 = stmt1.executeQuery();
                //ResultSet rs2 = stmt2.executeQuery();
                
                // 处理结果
                if (rs1.next()) {
                    System.out.println("Query 1: ID=" + rs1.getInt(1) + ", Test=" + rs1.getString(2));
                }
                
                //if (rs2.next()) {
                //    System.out.println("Query 2: Count=" + rs2.getInt(1));
                //}
                
                // 显式关闭资源
                rs1.close();
                //rs2.close();
                stmt1.close();
                //stmt2.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnectionTimeout() throws SQLException {
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                System.out.println("Connection established, waiting for potential timeout...");
                
                // 模拟长时间操作
                Thread.sleep(1000);
                
                // 测试连接是否仍然有效
                PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Connection is still valid");
                }
                
                rs.close();
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEquivalentSelect() throws SQLException {
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            try (Connection conn = DriverManager.getConnection(url, props)) {
                // 测试等值查询
                String sql = "SELECT id,test FROM my_table WHERE test = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);

                stmt.setString(1,"update_value");
                
                System.out.println("执行等值查询: " + sql );
                ResultSet rs = stmt.executeQuery();
                
                int count = 0;
                while (rs.next()) {
                    count++;
                    int id = rs.getInt("id");
                    String test = rs.getString("test");
                    System.out.println("查询结果 " + count + ": ID=" + id + ", Test=" + test);
                }
                System.out.println("总共查询到 " + count + " 条记录");
                
                rs.close();
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

