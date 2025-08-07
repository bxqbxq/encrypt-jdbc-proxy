package com.seu.jdbcproxy.core;

import com.seu.jdbcproxy.rewrite.EncryptionHelper;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public class ProxyResultSet  implements ResultSet {

    private final ResultSet realResultSet;
    private final EncryptionHelper encryptionHelper;

    public ProxyResultSet(ResultSet realResultSet, EncryptionHelper encryptionHelper) {
        this.realResultSet = realResultSet;
        this.encryptionHelper = encryptionHelper;
    }

    private boolean isTestColumn(int columnIndex) throws SQLException {
        // 假设 `test` 是第 2 列，可根据实际情况动态判断
        return "test".equalsIgnoreCase(realResultSet.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
//        if (isTestColumn(columnIndex)) { // 判断是否是 `test` 字段
//            try {
//                return encryptionHelper.decrypt(encryptedvalue);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to decrypt value for column index: " + columnIndex, e);
//            }
//        }
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                //System.out.println(111111);
                return encryptionHelper.decrypt(encryptedValue);  // 解密密文
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return encryptedValue; // 非 `test` 字段直接返回原始值
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        String encryptedValue = realResultSet.getString(columnLabel);
//        if ("test".equalsIgnoreCase(columnLabel)) { // 判断是否是 `test` 字段
//            try {
//                return encryptionHelper.decrypt(encryptedValue);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to decrypt value for column: " + columnLabel, e);
//            }
//        }
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return encryptionHelper.decrypt(encryptedValue);  // 解密密文
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return encryptedValue; // 非 `test` 字段直接返回原始值
    }

    @Override
    public boolean next() throws SQLException {
        return realResultSet.next();
    }

    @Override
    public void close() throws SQLException {
        realResultSet.close();
    }

    @Override
    public boolean wasNull() throws SQLException {
        return realResultSet.wasNull();
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Boolean.parseBoolean(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getBoolean(columnIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Byte.parseByte(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getByte(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Short.parseShort(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Integer.parseInt(encryptionHelper.decrypt(encryptedValue));  // 解密并转换为原始类型
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Long.parseLong(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Float.parseFloat(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Double.parseDouble(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getDouble(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return new BigDecimal(encryptionHelper.decrypt(encryptedValue)).setScale(scale, RoundingMode.HALF_UP);
            } catch (Exception e) {
                throw new SQLException("Error decrypting the BigDecimal value for column index with scale: " + columnIndex, e);
            }
        }
        return realResultSet.getBigDecimal(columnIndex, scale);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                // 解密后将解密的字符串转换为字节数组
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return decryptedValue.getBytes(StandardCharsets.UTF_8); // 以 UTF-8 字符集转换为字节数组
            } catch (Exception e) {
                throw new SQLException("Error decrypting the byte array for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getBytes(columnIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Date.valueOf(decryptedValue);  // 假设解密后的值是 "yyyy-mm-dd" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the date value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getDate(columnIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Time.valueOf(decryptedValue);  // 假设解密后的值是 "hh:mm:ss" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the time value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getTime(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Timestamp.valueOf(decryptedValue);  // 假设解密后的值是 "yyyy-mm-dd hh:mm:ss" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the timestamp value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getTimestamp(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        // 获取原始的 ASCII 字符流
        InputStream inputStream = realResultSet.getAsciiStream(columnIndex);

        // 判断数据是否需要解密
        if (inputStream != null) {
            try {
                // 读取流并转换为字符串
                byte[] bytes = inputStream.readAllBytes(); // 或者使用其它方式读取流
                String encryptedValue = new String(bytes, StandardCharsets.UTF_8);

                // 如果是加密的内容，解密它
                if (encryptionHelper.isCipherText(encryptedValue)) {
                    String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                    return new ByteArrayInputStream(decryptedValue.getBytes(StandardCharsets.UTF_8));
                } else {
                    // 如果不是加密内容，直接返回原始流
                    return new ByteArrayInputStream(bytes);
                }
            } catch (Exception e) {
                throw new SQLException("Error decrypting the ASCII stream for column index: " + columnIndex, e);
            }
        }
        return inputStream; // 返回原始流
        //return realResultSet.getAsciiStream(columnIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        // 获取原始的 Unicode 字符流
        InputStream inputStream = realResultSet.getUnicodeStream(columnIndex);

        if (inputStream != null) {
            try {
                byte[] bytes = inputStream.readAllBytes(); // 或者使用其它方式读取流
                String encryptedValue = new String(bytes, StandardCharsets.UTF_8);

                // 解密加密内容
                if (encryptionHelper.isCipherText(encryptedValue)) {
                    String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                    return new ByteArrayInputStream(decryptedValue.getBytes(StandardCharsets.UTF_8));
                } else {
                    return new ByteArrayInputStream(bytes);
                }
            } catch (Exception e) {
                throw new SQLException("Error decrypting the Unicode stream for column index: " + columnIndex, e);
            }
        }
        return inputStream; // 返回原始流
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        // 获取原始的二进制流
        InputStream inputStream = realResultSet.getBinaryStream(columnIndex);

        if (inputStream != null) {
            try {
                byte[] bytes = inputStream.readAllBytes(); // 或者使用其它方式读取流
                String encryptedValue = new String(bytes, StandardCharsets.UTF_8); // 假设二进制数据可以转换为字符串

                // 如果是加密数据，则解密
                if (encryptionHelper.isCipherText(encryptedValue)) {
                    String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                    return new ByteArrayInputStream(decryptedValue.getBytes(StandardCharsets.UTF_8));
                } else {
                    return new ByteArrayInputStream(bytes);
                }
            } catch (Exception e) {
                throw new SQLException("Error decrypting the Binary stream for column index: " + columnIndex, e);
            }
        }
        return inputStream; // 返回原始流
        //return realResultSet.getBinaryStream(columnIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(realResultSet.findColumn(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(realResultSet.findColumn(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(realResultSet.findColumn(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        String encryptedValue = realResultSet.getString(columnLabel);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return Integer.parseInt(encryptionHelper.decrypt(encryptedValue));  // 解密并转换为原始类型
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return realResultSet.getInt(columnLabel);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(realResultSet.findColumn(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(realResultSet.findColumn(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(realResultSet.findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        String encryptedValue = realResultSet.getString(columnLabel);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return new BigDecimal(encryptionHelper.decrypt(encryptedValue)).setScale(scale, RoundingMode.HALF_UP);
            } catch (Exception e) {
                throw new SQLException("Error decrypting the BigDecimal value for column index with scale: " + columnLabel, e);
            }
        }
        return realResultSet.getBigDecimal(columnLabel, scale);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(realResultSet.findColumn(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(realResultSet.findColumn(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(realResultSet.findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(realResultSet.findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return getAsciiStream(realResultSet.findColumn(columnLabel));
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return getUnicodeStream(realResultSet.findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(realResultSet.findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return realResultSet.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        realResultSet.clearWarnings();
    }

    @Override
    public String getCursorName() throws SQLException {
        return realResultSet.getCursorName();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return realResultSet.getMetaData();
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                //System.out.println(111);
                return decryptedValue;  // 返回解密后的对象，假设是 String 类型
            } catch (Exception e) {
                throw new SQLException("Error decrypting the object value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getObject(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(realResultSet.findColumn(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return realResultSet.findColumn(columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        // 获取原始的字符流
        Reader reader = realResultSet.getCharacterStream(columnIndex);

        if (reader != null) {
            try {
                // 将流读取为字符数组
                char[] chars = new char[reader.read()];
                reader.read(chars);  // 将字符流读取到字符数组

                // 将字符数组转换为字符串
                String encryptedValue = new String(chars);

                // 判断是否是加密的内容
                if (encryptionHelper.isCipherText(encryptedValue)) {
                    // 解密加密的数据
                    String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                    // 将解密后的字符数据返回为字符流
                    return new StringReader(decryptedValue);
                } else {
                    // 如果不是加密内容，直接返回原始字符流
                    return new CharArrayReader(chars);
                }
            } catch (Exception e) {
                throw new SQLException("Error decrypting the Character stream for column index: " + columnIndex, e);
            }
        }

        return reader; // 返回原始流
        //return realResultSet.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return getCharacterStream(realResultSet.findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                return new BigDecimal(encryptionHelper.decrypt(encryptedValue));
            } catch (Exception e) {
                throw new SQLException("Error decrypting the BigDecimal value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(realResultSet.findColumn(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return realResultSet.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return realResultSet.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return realResultSet.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return realResultSet.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        realResultSet.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        realResultSet.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return realResultSet.first();
    }

    @Override
    public boolean last() throws SQLException {
        return realResultSet.last();
    }

    @Override
    public int getRow() throws SQLException {
        return realResultSet.getRow();
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return realResultSet.absolute(row);
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return realResultSet.relative(rows);
    }

    @Override
    public boolean previous() throws SQLException {
        return realResultSet.previous();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        realResultSet.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return realResultSet.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        realResultSet.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return realResultSet.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return realResultSet.getType();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return realResultSet.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return realResultSet.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return realResultSet.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return realResultSet.rowDeleted();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        realResultSet.updateNull(columnIndex);
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        realResultSet.updateBoolean(columnIndex, x);
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        realResultSet.updateByte(columnIndex, x);
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        realResultSet.updateShort(columnIndex, x);
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        realResultSet.updateInt(columnIndex, x);
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        realResultSet.updateLong(columnIndex, x);
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        realResultSet.updateFloat(columnIndex, x);
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        realResultSet.updateDouble(columnIndex, x);
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        realResultSet.updateBigDecimal(columnIndex, x);
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        realResultSet.updateString(columnIndex, x);
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        realResultSet.updateBytes(columnIndex, x);
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        realResultSet.updateDate(columnIndex, x);
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        realResultSet.updateTime(columnIndex, x);
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        realResultSet.updateTimestamp(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        realResultSet.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        realResultSet.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        realResultSet.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        realResultSet.updateObject(columnIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        realResultSet.updateObject(columnIndex, x);
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        realResultSet.updateNull(columnLabel);
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        realResultSet.updateBoolean(columnLabel, x);
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        realResultSet.updateByte(columnLabel, x);
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        realResultSet.updateShort(columnLabel, x);
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        realResultSet.updateInt(columnLabel, x);
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        realResultSet.updateLong(columnLabel, x);
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        realResultSet.updateFloat(columnLabel, x);
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        realResultSet.updateDouble(columnLabel, x);
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        realResultSet.updateBigDecimal(columnLabel, x);
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        realResultSet.updateString(columnLabel, x);
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        realResultSet.updateBytes(columnLabel, x);
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        realResultSet.updateDate(columnLabel, x);
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        realResultSet.updateTime(columnLabel, x);
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        realResultSet.updateTimestamp(columnLabel, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        realResultSet.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        realResultSet.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        realResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        realResultSet.updateObject(columnLabel, x, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        realResultSet.updateObject(columnLabel, x);
    }

    @Override
    public void insertRow() throws SQLException {
        realResultSet.insertRow();
    }

    @Override
    public void updateRow() throws SQLException {
        realResultSet.updateRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        realResultSet.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        realResultSet.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        realResultSet.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        realResultSet.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        realResultSet.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return realResultSet.getStatement();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        // 获取原始对象
        Object object = realResultSet.getObject(columnIndex, map);

        // 如果对象是加密数据，则进行解密
        if (object != null && object instanceof String) {
            String encryptedValue = (String) object;

            // 检查是否是加密的数据
            if (encryptionHelper.isCipherText(encryptedValue)) {
                try {
                    // 解密数据
                    String decryptedValue = encryptionHelper.decrypt(encryptedValue);

                    // 返回解密后的对象（可能是字符串或其他类型）
                    return convertToRequiredType(decryptedValue,columnIndex, map);
                } catch (Exception e) {
                    throw new SQLException("Error decrypting the object for column index: " + columnIndex, e);
                }
            }
        }

        // 如果不是加密的内容，直接返回原始对象
        return object;
    }

    private Object convertToRequiredType(String decryptedValue,int columnIndex, Map<String, Class<?>> map) throws Exception {
        // 根据 map 中的类型进行转换
        if (map != null) {
            // 获取列的名称
            String columnName = realResultSet.getMetaData().getColumnName(columnIndex);

            // 获取列对应的目标类型
            Class<?> targetType = map.get(columnName);

            if (targetType != null) {
                // 转换为目标类型
                return convertToType(decryptedValue, targetType);
            }
        }

        return decryptedValue;  // 默认返回解密后的值
    }

    private Object convertToType(String decryptedValue, Class<?> targetType) {
        if (targetType == String.class) {
            return decryptedValue;
        } else if (targetType == Integer.class) {
            return Integer.parseInt(decryptedValue);
        } else if (targetType == Double.class) {
            return Double.parseDouble(decryptedValue);
        } else if (targetType == Date.class) {
            return Date.valueOf(decryptedValue); // assuming decrypted value is in the correct format
        }
        // 处理其他类型
        return decryptedValue; // 默认返回解密后的字符串
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return realResultSet.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return new javax.sql.rowset.serial.SerialBlob(decryptedValue.getBytes());
            } catch (Exception e) {
                throw new SQLException("Error decrypting the Blob value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getBlob(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return new javax.sql.rowset.serial.SerialClob(decryptedValue.toCharArray());
            } catch (Exception e) {
                throw new SQLException("Error decrypting the Clob value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getClob(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return realResultSet.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return getObject(realResultSet.findColumn(columnLabel), map);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return realResultSet.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return getBlob(realResultSet.findColumn(columnLabel));
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return getClob(realResultSet.findColumn(columnLabel));
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return realResultSet.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Date.valueOf(decryptedValue); // 假设解密后的值是 "yyyy-mm-dd" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the date value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(realResultSet.findColumn(columnLabel), cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Time.valueOf(decryptedValue); // 假设解密后的值是 "hh:mm:ss" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the time value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(realResultSet.findColumn(columnLabel), cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        String encryptedValue = realResultSet.getString(columnIndex);
        if (encryptedValue != null && encryptionHelper.isCipherText(encryptedValue)) {
            try {
                String decryptedValue = encryptionHelper.decrypt(encryptedValue);
                return Timestamp.valueOf(decryptedValue); // 假设解密后的值是 "yyyy-mm-dd hh:mm:ss" 格式
            } catch (Exception e) {
                throw new SQLException("Error decrypting the timestamp value for column index: " + columnIndex, e);
            }
        }
        return realResultSet.getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(realResultSet.findColumn(columnLabel), cal);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return realResultSet.getURL(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return realResultSet.getURL(columnLabel);
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        realResultSet.updateRef(columnIndex, x);
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        realResultSet.updateRef(columnLabel, x);
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        realResultSet.updateBlob(columnIndex, x);
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        realResultSet.updateBlob(columnLabel, x);
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        realResultSet.updateClob(columnIndex, x);
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        realResultSet.updateClob(columnLabel, x);
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        realResultSet.updateArray(columnIndex, x);
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        realResultSet.updateArray(columnLabel, x);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return realResultSet.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return realResultSet.getRowId(columnLabel);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        realResultSet.updateRowId(columnIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        realResultSet.updateRowId(columnLabel, x);
    }

    @Override
    public int getHoldability() throws SQLException {
        return realResultSet.getHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return realResultSet.isClosed();
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        realResultSet.updateNString(columnIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        realResultSet.updateNString(columnLabel, nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        realResultSet.updateNClob(columnIndex, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        realResultSet.updateNClob(columnLabel, nClob);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return realResultSet.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return realResultSet.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return realResultSet.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return realResultSet.getSQLXML(columnLabel);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        realResultSet.updateSQLXML(columnIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        realResultSet.updateSQLXML(columnLabel, xmlObject);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return realResultSet.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return realResultSet.getNString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return realResultSet.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return realResultSet.getNCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        realResultSet.updateNCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        realResultSet.updateNCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        realResultSet.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        realResultSet.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        realResultSet.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        realResultSet.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        realResultSet.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        realResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        realResultSet.updateBlob(columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        realResultSet.updateBlob(columnLabel, inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        realResultSet.updateClob(columnIndex, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        realResultSet.updateClob(columnLabel, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        realResultSet.updateNClob(columnIndex, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        realResultSet.updateNClob(columnLabel, reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        realResultSet.updateNCharacterStream(columnIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        realResultSet.updateNCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        realResultSet.updateAsciiStream(columnIndex, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        realResultSet.updateBinaryStream(columnIndex, x);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        realResultSet.updateCharacterStream(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        realResultSet.updateAsciiStream(columnLabel, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        realResultSet.updateBinaryStream(columnLabel, x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        realResultSet.updateCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        realResultSet.updateBlob(columnIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        realResultSet.updateBlob(columnLabel, inputStream);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        realResultSet.updateClob(columnIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        realResultSet.updateClob(columnLabel, reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        realResultSet.updateNClob(columnIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        realResultSet.updateNClob(columnLabel, reader);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return realResultSet.getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return realResultSet.getObject(columnLabel, type);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realResultSet.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realResultSet.isWrapperFor(iface);
    }
}
