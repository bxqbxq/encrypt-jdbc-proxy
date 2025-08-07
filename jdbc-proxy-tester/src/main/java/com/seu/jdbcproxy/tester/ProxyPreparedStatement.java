package com.seu.jdbcproxy.tester;


//import com.seu.jdbcproxy.core.ProxyConnection;
//import com.seu.jdbcproxy.core.ProxyResultSet;
import com.seu.jdbcproxy.ConfigLoader;
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

public class ProxyPreparedStatement implements PreparedStatement {

    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final String connId;
    private final String sql;
    private final ProxyConnection parentConn;

    private final Map<Integer, Object> params = new HashMap<>();
    //private final List<Object> params = new ArrayList<>();
    private boolean closed = false;
    private ProxyResultSet currentRs;
    private int updateCount = -1;

    private static final Logger LOG = Logger.getLogger("ProxyPreparedStatement");

    public ProxyPreparedStatement(ObjectInputStream in,
                                  ObjectOutputStream out,
                                  String connId,
                                  String sql,
                                  ProxyConnection parentConn) {
        this.in = in;
        this.out = out;
        this.connId = connId;
        this.sql = sql;
        this.parentConn = parentConn;
    }

    /* ------------------------------------------------------------
     * Internal helpers
     * ------------------------------------------------------------ */
    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("PreparedStatement already closed");
    }

    private Response send(MessageType type) throws SQLException {
        try {
            Request req = new Request();
            req.setType(type);
            req.setConnId(connId);
            req.setSql(sql);
            req.setParams(new HashMap<>(params));// 拷贝，防止并发修改


            out.writeObject(req);
            out.flush();

            Response resp = (Response) in.readObject();
            if (resp.getType() == MessageType.EXCEPTION) {
                SQLException e = resp.getException();
                throw (e != null ? e : new SQLException("Unknown error from proxy server"));
            }
            return resp;
        } catch (IOException | ClassNotFoundException ex) {
            throw new SQLException("I/O error communicating with proxy", ex);
        }
    }

    private static SQLFeatureNotSupportedException notSupported() {
        return new SQLFeatureNotSupportedException("Not supported by proxy driver.");
    }

    /* ------------------------------------------------------------
     * Parameter setters (只实现常用几种；其余可走 setObject)
     * ------------------------------------------------------------ */
    @Override
    public void setInt(int idx, int v) throws SQLException {
        params.put(idx, v);
    }

    @Override
    public void setLong(int idx, long v) throws SQLException {
        params.put(idx, v);
    }

    @Override
    public void setFloat(int idx, float x) throws SQLException {
        params.put(idx, x);
    }

    @Override
    public void setDouble(int idx, double v) throws SQLException {
        params.put(idx, v);
    }

    @Override
    public void setBigDecimal(int idx, BigDecimal x) throws SQLException {
        params.put(idx, x);
    }

    @Override
    public void setString(int idx, String v) throws SQLException {
        params.put(idx, v);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {

    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

    }

    @Override
    public void setBoolean(int idx, boolean v) throws SQLException {
        params.put(idx, v);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {

    }

    @Override
    public void setObject(int idx, Object val) throws SQLException {
        params.put(idx, val);
    }

    /* ------------------------------------------------------------
     * Execute APIs
     * ------------------------------------------------------------ */
    @Override
    public ResultSet executeQuery() throws SQLException {
        ensureOpen();
        Response resp = send(MessageType.EXEC_QUERY);
        Request req = new Request();
        req.setType(MessageType.EXEC_QUERY);
        req.setFetchSize(this.getFetchSize());

        this.updateCount = -1;
        this.currentRs = new ProxyResultSet(
                in, out, connId,
                resp.getRows(), resp.isHasMoreRows(),
                parentConn,resp.getColumnNames());

        //boolean enableExtraQuery = ConfigLoader.get("extra.enabled","false").equalsIgnoreCase("true");

        return currentRs;
    }

    @Override
    public int executeUpdate() throws SQLException {
        ensureOpen();
        Response resp = send(MessageType.EXEC_UPDATE);

        this.currentRs = null;
        this.updateCount = resp.getUpdateCount();
        return updateCount;
    }

    @Override
    public boolean execute() throws SQLException {
        ensureOpen();
        // 先发 EXEC_QUERY，若服务器返回 updateCount>=0 则视为 DML
        Response resp = send(MessageType.EXEC_QUERY);

        if (resp.getRows() != null && !resp.getRows().isEmpty()) {
            this.currentRs = new ProxyResultSet(
                    in, out, connId,
                    resp.getRows(), resp.isHasMoreRows(),
                    parentConn,resp.getColumnNames());
            this.updateCount = -1;
            return true;                     // 有结果集
        } else {
            this.currentRs = null;
            this.updateCount = resp.getUpdateCount();
            return false;                    // 返回更新计数
        }
    }

    /* ------------------------------------------------------------
     * Results & updateCount
     * ------------------------------------------------------------ */
    @Override
    public ResultSet getResultSet() {
        return currentRs;
    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public boolean getMoreResults() {
        return false;
    } // 不支持多结果集

    /* ------------------------------------------------------------
     * Batch (optional)
     * ------------------------------------------------------------ */
    @Override
    public void addBatch() throws SQLException {
        throw notSupported();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {

    }

    @Override
    public void clearParameters() {
        params.clear();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

    }

    /* ------------------------------------------------------------
     * Close
     * ------------------------------------------------------------ */
    @Override
    public void close() throws SQLException {
        if (closed) return;
        params.clear();
        if (currentRs != null) currentRs.close();
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    /* ------------------------------------------------------------
     * Meta / other simple getters
     * ------------------------------------------------------------ */
    @Override
    public int getMaxFieldSize() {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) { /* ignore */ }

    @Override
    public int getMaxRows() {
        return 0;
    }

    @Override
    public void setMaxRows(int max) { /* ignore */ }

    @Override
    public void setEscapeProcessing(boolean enable) { /* ignore */ }

    @Override
    public int getQueryTimeout() {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) { /* ignore */ }

    @Override
    public void cancel() { /* ignore */ }

    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() { /* ignore */ }

    @Override
    public void setCursorName(String name) { /* ignore */ }

    @Override
    public boolean execute(String sql) throws SQLException {
        throw notSupported();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw notSupported();
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw notSupported();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw notSupported();
    }

    @Override
    public void clearBatch() { /* ignore */ }

    @Override
    public int[] executeBatch() throws SQLException {
        throw notSupported();
    }

    @Override
    public Connection getConnection() {
        return parentConn;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    /* ------------------------------------------------------------
     * Generated keys / ResultSet type — 简化实现
     * ------------------------------------------------------------ */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw notSupported();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getFetchDirection() {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchDirection(int direction) { /* ignore */ }

    @Override
    public int getFetchSize() {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) { /* ignore */ }

    /* ------------------------------------------------------------
     * Wrapper
     * ------------------------------------------------------------ */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    /* ------------------------------------------------------------
     * Unimplemented setters for complex types
     * ------------------------------------------------------------ */
    @Override
    public ParameterMetaData getParameterMetaData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }

    @Override
    public void setArray(int i, Array x) throws SQLException {
        params.put(i, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    @Override
    public void setBlob(int i, Blob x) throws SQLException {
        params.put(i, x);
    }

    @Override
    public void setClob(int i, Clob x) throws SQLException {
        params.put(i, x);
    }

    @Override
    public void setNull(int i, int sqlType) throws SQLException {
        params.put(i, null);
    }
    // 依业务需要继续补 setDate / setTimestamp / setBigDecimal 等

    /* ------------------------------------------------------------
     * JDBC 4.0+ default methods (schema, closeOnCompletion, etc.)
     * ------------------------------------------------------------ */
    @Override
    public boolean isCloseOnCompletion() {
        return false;
    }

    @Override
    public void closeOnCompletion() { /* ignore */ }

    @Override
    public long getLargeUpdateCount() {
        return getUpdateCount();
    }

    @Override
    public void setLargeMaxRows(long max) { /* ignore */ }

    @Override
    public long getLargeMaxRows() {
        return 0;
    }

    /* 其它 JDBC 4+ 新增接口根据需要再补 */

}
