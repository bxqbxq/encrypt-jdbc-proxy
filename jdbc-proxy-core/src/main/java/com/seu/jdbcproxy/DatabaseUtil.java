package com.seu.jdbcproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);

    static {
        /* ProxyDriver 自带 META‑INF/services/java.sql.Driver，会被自动注册。
           保险起见，可手动加载一次（不会重复注册）。 */
        try {
            Class.forName("com.seu.jdbcproxy.ProxyDriver");
            System.out.println("加载成功！");
        }
        catch (ClassNotFoundException ignored) { }
    }

    public static Connection getConnection() throws SQLException {

        /* ------------------------- 读取配置 ------------------------- */
        // proxy 地址（默认本机 9999）
        String proxyUrl = ConfigLoader.get(
                "proxy.url",
                "jdbc:proxy://127.0.0.1:9999");

        Properties props = new Properties();
        // 真实库地址、用户名、密码放到 Properties
        props.setProperty("realUrl",  ConfigLoader.get("db.realUrl",
                "jdbc:mysql://localhost:3306/test_mysql"));
        props.setProperty("user",     ConfigLoader.get("db.user",     "root"));
        props.setProperty("password", ConfigLoader.get("db.password", "200261"));

        logger.info("Connecting via proxy: {}  →  {}", proxyUrl, props.getProperty("realUrl"));
        return DriverManager.getConnection(proxyUrl, props);
    }

    public static Connection getExtraDbConnection() throws SQLException {
        String url = ConfigLoader.get("extra.db.url");
        String user = ConfigLoader.get("extra.db.user");
        String password = ConfigLoader.get("extra.db.password");

        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 根据指定的数据库URL获取额外数据库连接
     * @param extraDbUrl 额外数据库的URL
     * @return 数据库连接
     * @throws SQLException 连接异常
     */
    public static Connection getExtraDbConnection(String extraDbUrl) throws SQLException {
        String user = ConfigLoader.get("extra.db.user", "root");
        String password = ConfigLoader.get("extra.db.password", "200261");
        
        logger.info("Connecting to extra database: {}", extraDbUrl);
        return DriverManager.getConnection(extraDbUrl, user, password);
    }
}
