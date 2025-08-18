package com.seu.jdbcproxy.core;

import com.seu.jdbcproxy.ConfigLoader;
import com.seu.jdbcproxy.DatabaseUtil;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Response;
import com.seu.jdbcproxy.rewrite.EncryptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.Proxy;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ProxyPreparedStatement implements PreparedStatement {
    private static final Logger logger = LoggerFactory.getLogger(ProxyPreparedStatement.class);

    private PreparedStatement realStatement;
    private final String originalSql;
    private final List<Object> parameterValues;
    private final EncryptionHelper encryptionHelper;
    private final Map<Integer, Object> params = new HashMap<>();
    ;


    public ProxyPreparedStatement(PreparedStatement realStatement, String originalSql) {

        this.realStatement = realStatement;
        this.originalSql = originalSql;
        this.parameterValues = new ArrayList<>();
        this.encryptionHelper = new EncryptionHelper();
    }

    public ProxyPreparedStatement(PreparedStatement realStatement, String originalSql, EncryptionHelper encryptionHelper) {
        this.realStatement = realStatement;
        this.originalSql = originalSql;
        this.parameterValues = new ArrayList<>();
        this.encryptionHelper = encryptionHelper;
    }

    private void encryptParametersForInsert() throws Exception {

        //获取占位符个数
        int parameterCount = realStatement.getParameterMetaData().getParameterCount();

        for (int i = 0; i < parameterValues.size(); i++) {
            if (i >= parameterCount) {
                continue;
            }

            Object paramValue = parameterValues.get(i);

            if (paramValue != null) {
                // 加密参数值
                String encryptedValue = encryptionHelper.encrypt(paramValue.toString());

                // 将加密后的值重新设置回 realStatement
                realStatement.setObject(i + 1, encryptedValue);
                logger.info("Encrypted value for parameter {}: {}", i + 1, encryptedValue);
            }
        }

    }

    private void encryptParametersForUpdate() throws Exception {
        //获取占位符个数
        int parameterCount = realStatement.getParameterMetaData().getParameterCount();

        for (int i = 0; i < parameterValues.size(); i++) {
            if (i >= parameterCount) {
                continue;
            }

            Object paramValue = parameterValues.get(i);

            if (paramValue != null && i != 1) { //假设 id 是第二个参数
                // 加密参数值
                String encryptedValue = encryptionHelper.encrypt(paramValue.toString());

                // 将加密后的值重新设置回 realStatement
                realStatement.setObject(i + 1, encryptedValue);
                logger.info("Encrypted value for parameter {}: {}", i + 1, encryptedValue);
            } else {
                realStatement.setObject(i + 1, paramValue);
            }
        }
    }

    private void encryptParametersForDelete() throws Exception {

        String sql = realStatement.toString().toUpperCase().trim();

        // 解析 SQL 获取表名，假设 DELETE SQL 的格式是 DELETE FROM <table> WHERE <condition>
        String tableName = sql.split("FROM")[1].split("WHERE")[0].trim();  // 提取表名
        String whereClause = sql.split("WHERE")[1].trim(); // 提取 WHERE 子句

        // 获取参数列表中的条件值
        List<Object> conditionValues = parameterValues;  // 假设删除条件是参数列表中的值


//        // 加密参数列表中所有非密文内容
//        for (int i = 0; i < parameterValues.size(); i++) {
//            Object value = parameterValues.get(i);
//            if (value != null) {
//                String strValue = value.toString();
//                if (encryptionHelper.isCipherText(strValue)) {
//                    logger.info("Parameter {} is already encrypted, skipping.", i + 1);
//                } else {
//                    String encrypted = encryptionHelper.encrypt(strValue);
//                    realStatement.setObject(i + 1, encrypted);
//                    logger.info("Encrypted parameter {}: {}", i + 1, encrypted);
//                }
//            }
//        }

        for (int i = 0; i < parameterValues.size(); i++) {
            Object conditionValue = parameterValues.get(i);

            if (conditionValue != null) {
                String encrypted = encryptionHelper.encrypt(conditionValue.toString());
                realStatement.setObject(i + 1, encrypted);
                logger.info("Force-encrypted condition {}: {}", i + 1, encrypted);
            }
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        logger.info("executeUpdate in ProxyPreparedStatement");
        //String sql = realStatement.toString().toUpperCase().trim();
        String sql = originalSql.toUpperCase().trim();
        try {
            if (sql.startsWith("INSERT")) {
                encryptParametersForInsert();
            }

            if (sql.startsWith("UPDATE")) {
                encryptParametersForUpdate();
            }

            if (sql.startsWith("DELETE")) {
                encryptParametersForDelete();
            }
        } catch (Exception e) {
            throw new SQLException("Error during parameter encryption", e);
        }

        // 继续执行原始 `executeUpdate`
        return realStatement.executeUpdate();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        logger.info("executeQuery in ProxyPreparedStatement");
        
        // 去除注释，只保留可执行 SQL
        if (originalSql.contains("--@extra.enabled=true")) { // 此时是额外查询
            String pureSql = originalSql.split("--@")[0].trim();
            PreparedStatement mainStmt = realStatement.getConnection().prepareStatement(pureSql);
            ResultSet resultSet = mainStmt.executeQuery();
            ProxyResultSet decryptedResultSet = new ProxyResultSet(resultSet, encryptionHelper);

            String extraTable = extractTag(originalSql, "--@extra.table=");
            String extraCols = extractTag(originalSql, "--@extra.columns=");

            if (!extraTable.isEmpty() && !extraCols.isEmpty()) {
                String extraSql = "SELECT " + extraCols + " FROM " + extraTable;
                
                // 获取额外数据库连接URL，优先从SQL注释中获取，然后从配置中获取
                String extraDbUrl = extractTag(originalSql, "--@extra.db.url=");
                if (extraDbUrl.isEmpty()) {
                    extraDbUrl = ConfigLoader.get("extra.db.url");
                }
                if (extraDbUrl == null || extraDbUrl.trim().isEmpty()) {
                    // 如果没有配置额外数据库URL，使用默认配置
                    extraDbUrl = "jdbc:mysql://localhost:3306/test_mysql";
                }
                
                try (
                        Connection extraConn = DatabaseUtil.getExtraDbConnection(extraDbUrl);
                        PreparedStatement preparedStatement = extraConn.prepareStatement(extraSql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        ResultSet rawextraRs = preparedStatement.executeQuery()
                        //ProxyResultSet extraRs = new ProxyResultSet(rawextraRs, encryptionHelper)
                ) {
                    ProxyResultSet extraRs = new ProxyResultSet(rawextraRs, encryptionHelper);
                    ResultSetMetaData meta = extraRs.getMetaData();
                    logger.info("[执行额外查询] 数据库: {}, 表: {}, 列: {}", extraDbUrl, extraTable, extraCols);
                    int colCount = meta.getColumnCount();
                    while (extraRs.next()) {
                        StringBuilder sb = new StringBuilder("[额外查询结果] ");
                        for (int i = 1; i <= colCount; i++) {
                            sb.append(meta.getColumnLabel(i)).append("=").append(extraRs.getString(i)).append(" ");
                        }
                        logger.info(sb.toString());
                    }
                }
            }
            return decryptedResultSet;
            
        } else if (originalSql.toUpperCase().trim().contains("WHERE")) { // 此时是等值查询
            // 等值查询：直接使用已经绑定参数的 realStatement 执行，避免丢失参数/加密
            logger.info("执行等值查询，使用已绑定的参数");
            ResultSet resultSet = realStatement.executeQuery();
            ProxyResultSet decryptedResultSet = new ProxyResultSet(resultSet, encryptionHelper);
            return decryptedResultSet;
            
        } else { // 此时是普通查询
            // 普通查询：直接使用 realStatement 执行
            logger.info("执行普通查询");
            ResultSet resultSet = realStatement.executeQuery();
            ProxyResultSet decryptedResultSet = new ProxyResultSet(resultSet, encryptionHelper);
            return decryptedResultSet;
        }
    }

    private String extractTag(String sql, String key) {
        int index = sql.indexOf(key);
        if (index == -1) return "";
        int end = sql.indexOf(" ", index + key.length());
        if (end == -1) {
            return sql.substring(index + key.length()).trim();
        }
        return sql.substring(index + key.length(), end).trim();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, null);
        realStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        logger.info("setInt called for index {}: {}", parameterIndex, x);
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        logger.info("setString called for index {}: {}", parameterIndex, x);
        
        // 确保参数列表足够大
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        
        // 使用set方法替换指定位置的参数
        parameterValues.set(parameterIndex - 1, x);
        
        String sql = originalSql.toUpperCase().trim();
        // 对于SELECT语句，需要根据查询类型决定是否加密参数
        if (sql.startsWith("SELECT")) {
            // 检查是否是等值查询的WHERE条件
            if (sql.contains("WHERE") && sql.contains("=")) {
                // 等值查询，需要加密参数以匹配数据库中的密文
                try {
                    String encrypted = encryptionHelper.encrypt(x);
                    logger.info("等值查询参数已加密: {} = {} -> {}", parameterIndex, x, encrypted);
                    realStatement.setString(parameterIndex, encrypted);
                    logger.info("插入之后的 sql : {} " + realStatement.toString());
                } catch (Exception e) {
                    throw new SQLException("加密参数失败", e);
                }
            } else {
                // 其他SELECT查询（如全表查询），不需要加密参数
                logger.info("普通查询参数，不加密: {} = {}", parameterIndex, x);
                realStatement.setString(parameterIndex, x);
            }
        } else {
            // 非SELECT语句，直接设置
            realStatement.setString(parameterIndex, x);
        }
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {

        realStatement.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setObject(parameterIndex, x, targetSqlType);


    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        // 确保参数列表足够大
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null);
        }
        
        // 使用set方法替换指定位置的参数，而不是add
        parameterValues.set(parameterIndex - 1, x);
        
        String sql = originalSql.toUpperCase().trim();
        // 对于SELECT语句，需要根据查询类型决定是否加密参数
        if (x instanceof String str && sql.startsWith("SELECT")) {
            // 检查是否是等值查询的WHERE条件
            if (sql.contains("WHERE") && sql.contains("=")) {
                // 等值查询，需要加密参数以匹配数据库中的密文
                try {
                    x = encryptionHelper.encrypt(x.toString());
                    logger.info("等值查询参数已加密: {} = {} -> {}", parameterIndex, str, x);
                } catch (Exception e) {
                    throw new SQLException("加密参数失败", e);
                }
            } else {
                // 其他SELECT查询（如全表查询），不需要加密参数
                logger.info("普通查询参数，不加密: {} = {}", parameterIndex, x);
            }
        }

        realStatement.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        return realStatement.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        realStatement.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {

        return realStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, null);
        realStatement.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return realStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, value);
        realStatement.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, value);
        realStatement.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, value);
        realStatement.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, inputStream);
        realStatement.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, xmlObject);
        realStatement.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, x);
        realStatement.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, value);
        realStatement.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, inputStream);
        realStatement.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        while (parameterValues.size() < parameterIndex) {
            parameterValues.add(null); // 扩展参数列表
        }
        parameterValues.set(parameterIndex - 1, reader);
        realStatement.setNClob(parameterIndex, reader);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return realStatement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return realStatement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        realStatement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return realStatement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {

        realStatement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return realStatement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        realStatement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        realStatement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return realStatement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        realStatement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        realStatement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return realStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        realStatement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        realStatement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return realStatement.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return realStatement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return realStatement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return realStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        realStatement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return realStatement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        realStatement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return realStatement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return realStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return realStatement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        realStatement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        realStatement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return realStatement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return realStatement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return realStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return realStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return realStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return realStatement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return realStatement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return realStatement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return realStatement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return realStatement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return realStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return realStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        realStatement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return realStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        realStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return realStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realStatement.isWrapperFor(iface);
    }
}
