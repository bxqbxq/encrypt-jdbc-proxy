package com.seu.jdbcproxy;

import com.seu.jdbcproxy.tester.ProxyDriver;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Properties;

public class SimpleTest {

    @Test
    public void simpleSelct() throws SQLException {
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

            Connection conn = DriverManager.getConnection(url, props);
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
            conn.close();

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

            Connection conn = DriverManager.getConnection(url, props);
            String sql = "delete from my_table where test = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, "张三五");

            int rs = preparedStatement.executeUpdate();
            if (rs > 0) {
                System.out.println("Successfully executed");
            } else {
                System.out.println("Failed to execute");
            }

            preparedStatement.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void enquivalentSelect() throws SQLException {
        try {
            DriverManager.registerDriver(new ProxyDriver());

            String url = "jdbc:proxy://localhost:9999?realUrl=jdbc:mysql://127.0.0.1:3306/test_mysql";
            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("password", "200261");

            Connection conn = DriverManager.getConnection(url, props);
            String sql = "select id,test from my_table where test = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, "test try");

            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);       //根据不同的查询字段进行修改
                String test = rs.getString(2);
                System.out.println("ID: " + id + "  Test: " + test);
            }

            preparedStatement.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

    }
}

