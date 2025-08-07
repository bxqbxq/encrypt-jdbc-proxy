package com.seu.jdbcproxy;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class FunctionTests {
//    private static final Logger logger = LoggerFactory.getLogger(FunctionTests.class);
//
//    @Test
//    public void showTableDetailsTest_to_mysql() { // 查询my_tables表中的test字段，并可以将数据库中的密文解密输出
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            //logger.info("Proxy driver registered");
//
//            // 设置连接属性
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//
//            // 使用代理 URL 建立连接
//            //String url = "jdbc:mysql://jdbc:postgresql://localhost:5432/test_postgres:3306/mysql";
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//
//            //logger.info("Attempting to connect with URL: {}", url);
//
//            // 直接使用 ProxyDriver 的 connect 方法
//            Connection conn = proxyDriver.connect(url, props);
//
//            if (conn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//
//            // 执行查询
//            logger.info("Executing query...");
//            String sql = "select id,test from my_table";  //根据具体需求进行修改
//            //String sql = "select id,test from my_table where test = " + Encrypt.encrypt("test try");
//            PreparedStatement preparedStatement = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
//            ResultSet rs = preparedStatement.executeQuery();
//
//            while (rs.next()) {
//                int id = rs.getInt(1);       //根据不同的查询字段进行修改
//                String test = rs.getString(2);
//                System.out.println("ID: " + id + "  Test: " + test); ;
//            }
//
//            // 7关闭资源
//            rs.close();
//            preparedStatement.close();
//            conn.close();
//
//        } catch (Exception e) {
//            logger.error("Error occurred:", e);
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void  insertValueAutoEncryptTest_to_mysql() {  //实现加密插入
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            //logger.info("Proxy driver registered");
//
//            // 设置连接属性
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//
//            // 使用代理 URL 建立连接
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//
//            //logger.info("Attempting to connect with URL: {}", url);
//
//            // 直接使用 ProxyDriver 的 connect 方法
//            Connection conn = proxyDriver.connect(url, props);
//
//            if (conn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//            //logger.info("Connection established successfully");
//            //logger.info("Connection class: {}", conn.getClass().getName());
//
//            // 执行查询
//            logger.info("Executing Insert...");
//            String sql = "insert into my_table (test) values (?)";
//            PreparedStatement preparedStatement = conn.prepareStatement(sql);
//            preparedStatement.setString(1,"test try");
//
//
//            int rs = preparedStatement.executeUpdate();
//
//
//            if (rs > 0){
//                logger.info("添加成功");
//            }else {
//                logger.error("添加失败");
//            }
//
//            // 关闭资源
//            preparedStatement.close();
//            conn.close();
//
//        } catch (Exception e) {
//            logger.error("Error occurred:", e);
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void updateValueAutoEncryptTest_to_mysql() {  //实现更新操作
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            //logger.info("Proxy driver registered");
//
//            // 设置连接属性
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//
//            // 使用代理 URL 建立连接
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//
//            //logger.info("Attempting to connect with URL: {}", url);
//
//            // 直接使用 ProxyDriver 的 connect 方法
//            Connection conn = proxyDriver.connect(url, props);
//
//            if (conn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//            //logger.info("Connection established successfully");
//            //logger.info("Connection class: {}", conn.getClass().getName());
//
//            // 执行查询
//            logger.info("Executing Update...");
//            String sql = "update my_table set test = ? where id = ?";
//            PreparedStatement preparedStatement = conn.prepareStatement(sql);
//            preparedStatement.setString(1,"msyql");
//            preparedStatement.setInt(2,16);
//
//            int rs = preparedStatement.executeUpdate();
//
//
//            if (rs > 0){
//                logger.info("修改成功");
//            }else {
//                logger.error("修改失败");
//            }
//
//            // 关闭资源
//            preparedStatement.close();
//            conn.close();
//
//        } catch (Exception e) {
//            logger.error("Error occurred:", e);
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void deleteValueAutoEncryptTest_to_mysql(){
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            //logger.info("Proxy driver registered");
//
//            // 设置连接属性
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//
//            // 使用代理 URL 建立连接
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//
//            //logger.info("Attempting to connect with URL: {}", url);
//
//            // 直接使用 ProxyDriver 的 connect 方法
//            Connection conn = proxyDriver.connect(url, props);
//
//            if (conn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//            //logger.info("Connection established successfully");
//            //logger.info("Connection class: {}", conn.getClass().getName());
//
//            // 执行查询
//            logger.info("Executing delete...");
//            //String sql = "delete from my_table where id = ?";
//            String sql = "delete from my_table where test = ?";
//            PreparedStatement preparedStatement = conn.prepareStatement(sql);
//            //preparedStatement.setInt(1,13);
//            preparedStatement.setString(1,"delete try");
//
//            int rs = preparedStatement.executeUpdate();
//
//
//            if (rs > 0){
//                logger.info("删除成功");
//            }else {
//                logger.error("删除失败");
//            }
//
//            // 关闭资源
//            preparedStatement.close();
//            conn.close();
//
//        } catch (Exception e) {
//            logger.error("Error occurred:", e);
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void EquivalentSelectTest_to_mysql() { // 实现 等值 查询my_tables表中的test字段, 只能等值查明文
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            //logger.info("Proxy driver registered");
//
//            // 设置连接属性
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//
//            // 使用代理 URL 建立连接
//            //String url = "jdbc:mysql://jdbc:postgresql://localhost:5432/test_postgres:3306/mysql";
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//
//            //logger.info("Attempting to connect with URL: {}", url);
//
//            // 直接使用 ProxyDriver 的 connect 方法
//            Connection conn = proxyDriver.connect(url, props);
//
//            if (conn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//            //logger.info("Connection established successfully");
//            //logger.info("Connection class: {}", conn.getClass().getName());
//
//            // 执行查询
//            logger.info("Executing query...");
//            String sql = "select id,test from my_table where test = ?" ;
//            //PreparedStatement preparedStatement = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
//            PreparedStatement preparedStatement = conn.prepareStatement(sql);
//            String searchText = "test try";  // 你想查询的明文
//
//            preparedStatement.setString(1, searchText);
//            ResultSet rs = preparedStatement.executeQuery();
//
//            while (rs.next()) {
//                int id = rs.getInt(1);
//                String test = rs.getString(2);
//                logger.info("ID: {}, Test: {}", id,test);
//                //logger.info("Test: {}", test);
//            }
//
//            // 7关闭资源
//            rs.close();
//            preparedStatement.close();
//            conn.close();
//
//        } catch (Exception e) {
//            logger.error("Error occurred:", e);
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testExecuteAnotherSql(){    //未完善
//        try {
//            // 1. 注册驱动并建立连接
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "200261");
//            String url = "jdbc:mysql://localhost:3306/test_mysql";
//            Connection conn = proxyDriver.connect(url, props);
//
//            // 2. 执行插入（触发自动修改）
//            String insertSql = "INSERT INTO my_table (test) VALUES (?)";
//            DatabaseProxy.proxyExecute(insertSql, url, "auto_update_test",9);
//
//
//            // 4. 清理资源
//            conn.close();
//        } catch (Exception e) {
//            logger.error("测试异常: " + e.getMessage());
//        }
//    }

}




