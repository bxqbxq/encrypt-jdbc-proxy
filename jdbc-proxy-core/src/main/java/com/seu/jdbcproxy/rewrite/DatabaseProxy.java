package com.seu.jdbcproxy.rewrite;
//看在执行select语句前，能不能通过代理，自动再执行一个别的语句

//import com.seu.jdbcproxy.ProxyDriver;


public class DatabaseProxy {

//    private static final Logger logger = LoggerFactory.getLogger(DatabaseProxy.class);
////
////    // 主数据库配置
////    private static final String MAIN_DB_URL = "jdbc:mysql://localhost:3306/company_db";
////    private static final String MAIN_DB_USER = "root";
////    private static final String MAIN_DB_PASSWORD = "123456";
//
//    // 备用数据库配置 +++++++
//    private static final String BACKUP_DB_URL = "jdbc:mysql://localhost:3306/test_mysql";
//    private static final String BACKUP_DB_USER = "root";
//    private static final String BACKUP_DB_PASSWORD = "200261";
//
//    // 获取主数据库连接 没用了
////    private static ProxyConnection getMainDbConnection() throws SQLException {
////
////        return new ProxyConnection(DriverManager.getConnection(MAIN_DB_URL, MAIN_DB_USER, MAIN_DB_PASSWORD));
////    }
//
//    // 获取备用数据库连接
//    private static ProxyConnection getBackupDbConnection() throws SQLException {
//        return new ProxyConnection(DriverManager.getConnection(BACKUP_DB_URL, BACKUP_DB_USER, BACKUP_DB_PASSWORD));
//    }
//
//
//
//    // 备用数据库查询函数
//    public static void queryBackupDb(String sql) {
//        try (ProxyConnection proxyconnection = getBackupDbConnection();
//             Statement proxystatement = proxyconnection.createStatement()) {
//            ResultSet backupResult = proxystatement.executeQuery(sql);
//            while (backupResult != null && backupResult.next()) {
//                System.out.println("Backup Result: " + backupResult.getString("test"));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // 主代理方法，动态调用备用数据库查询
//    public static void proxyExecute(String sql,String url,String value) {
//        try {
//
//            // 注册代理驱动
//            ProxyDriver proxyDriver = new ProxyDriver();
//            DriverManager.registerDriver(proxyDriver);
//            logger.info("Proxy driver registered");
//            //下面是主数据库的连接
//            Properties main_props = new Properties();
//            main_props.setProperty("user", "root");
//            main_props.setProperty("password", "200261");
//
//            ProxyConnection proxyconn = new ProxyConnection(proxyDriver.connect(url, main_props));
//
//            if (proxyconn == null) {
//                logger.error("Connection is null!");
//                return;
//            }
//            logger.info("Connection established successfully");
//            logger.info("Connection class: {}", proxyconn.getClass().getName());
//
//            // 执行查询
//
//            if (sql.trim().toUpperCase().startsWith("SELECT")) {
////                ProxyPreparedStatement proxypreparedStatement = new ProxyPreparedStatement(proxyconn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE),sql);
//
//
//                Connection conn = proxyDriver.connect(url, main_props);
//                PreparedStatement preparedStatement = conn.prepareStatement(sql);
//                preparedStatement.setString(1,"test try123");
//                ProxyResultSet proxyrs = new ProxyResultSet(preparedStatement.executeQuery(),new EncryptionHelper());
//                //在查询主数据库前，调用备用数据库查询，这里写的是查询主数据库，但是装作是查询副数据库；
//                System.out.println("Querying backup database...");
//                queryBackupDb("SELECT * FROM my_table");
//
//                logger.info("Executing query...");
//
//                System.out.println("Querying main database...");
//
//                while (proxyrs.next()) {
//                    int id = proxyrs.getInt(1);
//                    String test = proxyrs.getString(2);
//                    logger.info("ID: {}", id);
//                    logger.info("Test: {}", test);
//                }
//
//                proxyrs.close();
//                preparedStatement.close();
//                proxyconn.close();
//            } else {
//                logger.info("Executing Insert...");
//                ProxyPreparedStatement proxypreparedStatement = new ProxyPreparedStatement(proxyconn.prepareStatement(sql),sql);
//                proxypreparedStatement.setString(1,value);
//                int rs = proxypreparedStatement.executeUpdate();
//
//                if (rs > 0){
//                    logger.info("添加成功");
//                }else {
//                    logger.error("添加失败");
//                }
//                proxypreparedStatement.close();
//                proxyconn.close();
//
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
}

