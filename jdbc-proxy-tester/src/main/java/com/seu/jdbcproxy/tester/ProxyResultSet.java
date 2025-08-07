package com.seu.jdbcproxy.tester;

//import com.seu.jdbcproxy.core.ProxyConnection;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Logger;

/**
 * Proxy ResultSet：把服务器返回的行缓存到本地；游标前移时如需更多数据再 FETCH_ROWS。
 */
public class ProxyResultSet implements ResultSet {

    /* ------------------------------------------------------------
     * Immutable fields
     * ------------------------------------------------------------ */
    private final ObjectInputStream  in;
    private final ObjectOutputStream out;
    private final String connId;
    private final ProxyConnection parentConn;
    private final List<String> columnNames;

    /* ------------------------------------------------------------
     * Cursor state
     * ------------------------------------------------------------ */
    private final List<List<Object>> buffer;  // 当前批次
    private Iterator<List<Object>>   iter;    // buffer's iterator
    private List<Object>             currentRow; // 指向 iter 当前元素
    private boolean hasMore;    // 服务器端还有剩余行
    private boolean closed = false;
    private int rowIndex = 0;   // 1‑based row number, per JDBC

    private static final int DEFAULT_FETCH_SIZE = 100;
    private static final Logger LOG = Logger.getLogger("TcpProxyResultSet");

    /* ------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------ */
    public ProxyResultSet(ObjectInputStream in,
                             ObjectOutputStream out,
                             String connId,
                             List<List<Object>> firstRows,
                             boolean hasMore,
                             ProxyConnection parentConn,
                             List<String> columnNames) {
        this.in   = in;
        this.out  = out;
        this.connId = connId;
        this.buffer = new ArrayList<>(firstRows);
        this.iter   = buffer.iterator();
        this.hasMore = hasMore;
        this.parentConn = parentConn;
        this.columnNames = columnNames;
    }

    /* ------------------------------------------------------------
     * Internal helpers
     * ------------------------------------------------------------ */
    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("ResultSet closed");
    }

    private void fetchNextBatch() throws SQLException {
        if (!hasMore) return;
        try {
            Request q = new Request();
            q.setType(MessageType.FETCH_ROWS);
            q.setConnId(connId);
            q.setFetchSize(this.getFetchSize());
            //q.setFetchSize(DEFAULT_FETCH_SIZE);

            out.writeObject(q);
            out.flush();

            Response resp = (Response) in.readObject();
            if (resp.getType() == MessageType.EXCEPTION) {
                SQLException e = resp.getException();
                throw (e != null ? e : new SQLException("Error fetching rows from proxy"));
            }

            buffer.clear();
            buffer.addAll(resp.getRows());
            iter = buffer.iterator();
            hasMore = resp.isHasMoreRows();

        } catch (IOException | ClassNotFoundException ex) {
            throw new SQLException("I/O error during FETCH_ROWS", ex);
        }
    }

    private Object getCol(int columnIndex) throws SQLException {
        ensureOpen();
        if (currentRow == null)
            throw new SQLException("No current row (did you call next()?)");

        if (columnIndex < 1 || columnIndex > currentRow.size())
            throw new SQLException("Column index out of bounds: " + columnIndex);

        return currentRow.get(columnIndex - 1);
    }

    private static SQLFeatureNotSupportedException notSupported() {
        return new SQLFeatureNotSupportedException("Not supported by proxy driver.");
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    /* ------------------------------------------------------------
     * Cursor movement
     * ------------------------------------------------------------ */
    @Override
    public boolean next() throws SQLException {
        ensureOpen();

        // 1) 如果 buffer 还有行
        if (iter.hasNext()) {
            currentRow = iter.next();
            rowIndex++;
            return true;
        }

        // 2) 尝试从服务器 fetch
        if (hasMore) {
            fetchNextBatch();
            if (iter.hasNext()) {
                currentRow = iter.next();
                rowIndex++;
                return true;
            }
        }
        // 3) no more rows
        currentRow = null;
        return false;
    }

    @Override public void close() {
        closed = true;
        buffer.clear();
    }

    @Override public boolean isClosed() { return closed; }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {

    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {

    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return "";
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return "";
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {

    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {

    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return null;
    }

    /* ------------------------------------------------------------
     * Column getters (常用三种；其余走 getObject + cast)
     * ------------------------------------------------------------ */
    @Override
    public String getString(int columnIndex) throws SQLException {
        Object val = getCol(columnIndex);
        return (val != null ? val.toString() : null);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Object val = getCol(columnIndex);
        return (val instanceof Number n) ? n.intValue()
                : (val == null ? 0 : Integer.parseInt(val.toString()));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        Object val = getCol(columnIndex);
        return (val instanceof Number n) ? n.longValue()
                : (val == null ? 0L : Long.parseLong(val.toString()));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return null;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return new byte[0];
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object val = getCol(columnIndex);
        return (val instanceof Boolean b) ? b : Boolean.parseBoolean(String.valueOf(val));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return 0;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return 0;
    }

    @Override public Object getObject(int columnIndex) throws SQLException {
        return getCol(columnIndex);
    }

    /* 简化：按列名的实现直接调用列索引获取（生产环境可缓存列名->索引映射） */
    @Override public String  getString (String col) throws SQLException { return getString(findCol(col)); }
    @Override public int     getInt    (String col) throws SQLException { return getInt(findCol(col)); }
    @Override public long    getLong   (String col) throws SQLException { return getLong(findCol(col)); }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return null;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return new byte[0];
    }

    @Override public boolean getBoolean(String col) throws SQLException { return getBoolean(findCol(col)); }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return 0;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return 0;
    }

    @Override public Object  getObject (String col) throws SQLException { return getObject(findCol(col)); }

    /* ------------------------------------------------------------
     * findCol: 简化 demo，在真实项目可携带列名元数据
     * ------------------------------------------------------------ */
    private int findCol(String columnLabel) throws SQLException {
        //throw notSupported();  // 若需按列名访问，可在 Response 中携带列名数组
        if (columnNames == null) {
            throw new SQLException("Column names metadata not available");
        }
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnLabel.equalsIgnoreCase(columnNames.get(i))) {
                return i + 1; // JDBC列索引从1开始
            }
        }
        throw new SQLException("Column not found: " + columnLabel);
    }

    /* ------------------------------------------------------------
     * Cursor / meta info
     * ------------------------------------------------------------ */
    @Override public boolean wasNull() { return false; }
    @Override public int getRow() { return rowIndex; }

    @Override
    public boolean absolute(int row) throws SQLException {
        return false;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return false;
    }

    @Override
    public boolean previous() throws SQLException {
        return false;
    }

    @Override public void beforeFirst() throws SQLException { throw notSupported(); }
    @Override public void afterLast()  throws SQLException { throw notSupported(); }
    @Override public boolean first()   throws SQLException { throw notSupported(); }
    @Override public boolean last()    throws SQLException { throw notSupported(); }
    @Override public int findColumn(String columnLabel) throws SQLException { return findCol(columnLabel); }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return false;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return false;
    }

    @Override
    public boolean isFirst() throws SQLException {
        return false;
    }

    @Override
    public boolean isLast() throws SQLException {
        return false;
    }

    /* ------------------------------------------------------------
     * Update methods —— ResultSet 是只读光标，全部不支持
     * ------------------------------------------------------------ */
    @Override public void updateInt(int columnIndex, int x) throws SQLException { throw notSupported(); }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {

    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {

    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {

    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {

    }

    @Override public void updateString(int columnIndex, String x) throws SQLException { throw notSupported(); }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {

    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {

    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {

    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {

    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {

    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {

    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {

    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {

    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {

    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {

    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {

    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {

    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {

    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {

    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {

    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {

    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {

    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {

    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {

    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {

    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {

    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {

    }

    @Override
    public void insertRow() throws SQLException {

    }

    @Override
    public void updateRow() throws SQLException {

    }

    @Override
    public void deleteRow() throws SQLException {

    }

    @Override
    public void refreshRow() throws SQLException {

    }

    @Override
    public void cancelRowUpdates() throws SQLException {

    }

    @Override
    public void moveToInsertRow() throws SQLException {

    }

    @Override
    public void moveToCurrentRow() throws SQLException {

    }
    // 其余 updateXxx 同理，全抛 notSupported()

    /* ------------------------------------------------------------
     * MetaData
     * ------------------------------------------------------------ */
//    @Override public ResultSetMetaData getMetaData() throws SQLException {
//        //throw notSupported();
//
//    }
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new ResultSetMetaData() {
            // 获取列数
            @Override
            public int getColumnCount() throws SQLException {
                return columnNames.size();
            }

            // 获取列标签（推荐使用）
            @Override
            public String getColumnLabel(int column) throws SQLException {
                validateColumnIndex(column);
                return columnNames.get(column - 1);
            }

            // 获取列名
            @Override
            public String getColumnName(int column) throws SQLException {
                return columnNames.get(column - 1);
            }

            // 获取列类型 (JDBC 类型代码)
            @Override
            public int getColumnType(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 VARCHAR 类型
                return Types.VARCHAR;
            }

            // 获取列类型名称
            @Override
            public String getColumnTypeName(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 "VARCHAR"
                return "VARCHAR";
            }

            // 判断列是否可为空
            @Override
            public int isNullable(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认假设所有列都可为空
                return ResultSetMetaData.columnNullable;
            }

            // 获取列显示宽度
            @Override
            public int getColumnDisplaySize(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 50 个字符宽度
                return 50;
            }

            // 获取列精度（数字类型）
            @Override
            public int getPrecision(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 0
                return 0;
            }

            // 获取列小数位数
            @Override
            public int getScale(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 0
                return 0;
            }

            // 获取表名
            @Override
            public String getTableName(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回空字符串
                return "";
            }

            // 获取列所属的目录名称
            @Override
            public String getCatalogName(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回空字符串
                return "";
            }

            // 获取列所属的模式名称
            @Override
            public String getSchemaName(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回空字符串
                return "";
            }

            // 检查列是否区分大小写
            @Override
            public boolean isCaseSensitive(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 true
                return true;
            }

            // 检查列是否可用于 WHERE 子句
            @Override
            public boolean isSearchable(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 true
                return true;
            }

            // 检查列是否为货币值
            @Override
            public boolean isCurrency(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 false
                return false;
            }

            // 检查列是否为有符号数
            @Override
            public boolean isSigned(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 false
                return false;
            }

            // 检查列是否为自增列
            @Override
            public boolean isAutoIncrement(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 false
                return false;
            }

            // 检查列是否为只读
            @Override
            public boolean isReadOnly(int column) throws SQLException {
                validateColumnIndex(column);
                // 在结果集中默认为只读
                return true;
            }

            // 检查列是否可写
            @Override
            public boolean isWritable(int column) throws SQLException {
                validateColumnIndex(column);
                // 在结果集中默认为不可写
                return false;
            }

            // 检查列是否确定可写
            @Override
            public boolean isDefinitelyWritable(int column) throws SQLException {
                validateColumnIndex(column);
                // 默认返回 false
                return false;
            }

            @Override
            public String getColumnClassName(int column) throws SQLException {
                validateColumnIndex(column);
                return Object.class.getName();
            }

            // 未实现的方法抛出异常
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLFeatureNotSupportedException("unwrap not supported");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }

            // 验证列索引是否有效
            private void validateColumnIndex(int column) throws SQLException {
                if (column < 1 || column > columnNames.size()) {
                    throw new SQLException("无效的列索引: " + column +
                            ". 有效范围: 1-" + columnNames.size());
                }
            }
        };
    }

    /* ------------------------------------------------------------
     * Fetch size / direction — 简单实现
     * ------------------------------------------------------------ */
    @Override public int getFetchSize() { return DEFAULT_FETCH_SIZE; }

    @Override
    public int getType() throws SQLException {
        return 0;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {

    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {

    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {

    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {

    }

    @Override public void setFetchSize(int rows) { /* ignore */ }
    @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchDirection(int direction) { /* ignore */ }

    /* ------------------------------------------------------------
     * Statement / Connection back‑refs
     * ------------------------------------------------------------ */
    @Override public Statement getStatement() { return null; }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {

    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {

    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {

    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {

    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {

    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {

    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {

    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {

    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {

    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }

    /* ------------------------------------------------------------
     * The rest of ResultSet methods are not implemented for brevity.
     * They can be added as needed following similar pattern.
     * ------------------------------------------------------------ */

    /* Auto‑generated stubs throwing notSupported() —— to save space, only a few shown */
    @Override public Date getDate(int columnIndex) throws SQLException { throw notSupported(); }
    @Override public Time getTime(int columnIndex) throws SQLException { throw notSupported(); }
    @Override public Timestamp getTimestamp(int columnIndex) throws SQLException { throw notSupported(); }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override public Date getDate(String columnLabel) throws SQLException { throw notSupported(); }
    @Override public Time getTime(String columnLabel) throws SQLException { throw notSupported(); }
    @Override public Timestamp getTimestamp(String columnLabel) throws SQLException { throw notSupported(); }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public String getCursorName() throws SQLException {
        return "";
    }



}