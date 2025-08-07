package com.seu.jdbcproxy.tester;

//import com.seu.jdbcproxy.core.ProxyConnection;
//import com.seu.jdbcproxy.core.ProxyResultSet;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Statement 代理：把 executeQuery/executeUpdate/execute 的 SQL 文本
 * 序列化成 Request 发送给 ProxyServer。
 */
public class ProxyStatement implements Statement {

    /* ------------------------------------------------------------
     * Immutable fields
     * ------------------------------------------------------------ */
    private final ObjectInputStream  in;
    private final ObjectOutputStream out;
    private final String connId;
    private final ProxyConnection parentConn;

    /* ------------------------------------------------------------
     * State
     * ------------------------------------------------------------ */
    private boolean closed = false;
    private int maxRows = 0;
    private int queryTimeout = 0;
    private int updateCount = -1;
    private ProxyResultSet currentRs;

    /* ------------------------------------------------------------
     * Batch
     * ------------------------------------------------------------ */
    private final List<String> batch = new ArrayList<>();

    private static final Logger LOG = Logger.getLogger("TcpProxyStatement");

    /* ------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------ */
    public ProxyStatement(ObjectInputStream in,
                             ObjectOutputStream out,
                             String connId,
                             ProxyConnection parentConn) {
        this.in = in;
        this.out = out;
        this.connId = connId;
        this.parentConn = parentConn;
    }

    /* ------------------------------------------------------------
     * Internal helpers
     * ------------------------------------------------------------ */
    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("Statement closed");
    }

    private Response send(String sql, MessageType type) throws SQLException {
        try {
            Request req = new Request();
            req.setType(type);
            req.setConnId(connId);
            req.setSql(sql);
            req.setFetchSize(maxRows);    // 若有 maxRows，可在服务器侧截断

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
     * Execute APIs
     * ------------------------------------------------------------ */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        ensureOpen();
        Response resp = send(sql, MessageType.EXEC_QUERY);

        this.updateCount = -1;
        this.currentRs   = new ProxyResultSet(
                in, out, connId,
                resp.getRows(), resp.isHasMoreRows(),
                parentConn,resp.getColumnNames());
        return currentRs;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        ensureOpen();
        Response resp = send(sql, MessageType.EXEC_UPDATE);

        this.currentRs   = null;
        this.updateCount = resp.getUpdateCount();
        return updateCount;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        ensureOpen();
        Response resp = send(sql, MessageType.EXEC_QUERY);

        if (resp.getRows() != null && !resp.getRows().isEmpty()) {
            this.currentRs = new ProxyResultSet(
                    in, out, connId,
                    resp.getRows(), resp.isHasMoreRows(),
                    parentConn,resp.getColumnNames());
            this.updateCount = -1;
            return true;            // 有结果集
        } else {
            this.currentRs   = null;
            this.updateCount = resp.getUpdateCount();
            return false;           // DML
        }
    }

    /* ------------------------------------------------------------
     * Batch support（仅支持批量 DML）
     * ------------------------------------------------------------ */
    @Override
    public void addBatch(String sql) throws SQLException {
        ensureOpen();
        batch.add(sql);
    }

    @Override
    public void clearBatch() { batch.clear(); }

    @Override
    public int[] executeBatch() throws SQLException {
        ensureOpen();
        int[] counts = new int[batch.size()];
        for (int i = 0; i < batch.size(); i++) {
            counts[i] = executeUpdate(batch.get(i));
        }
        clearBatch();
        return counts;
    }

    /* ------------------------------------------------------------
     * Result & update count getters
     * ------------------------------------------------------------ */
    @Override public ResultSet getResultSet() { return currentRs; }
    @Override public int getUpdateCount() { return updateCount; }
    @Override public boolean getMoreResults() { return false; } // 不支持多结果集

    /* ------------------------------------------------------------
     * Close
     * ------------------------------------------------------------ */
    @Override
    public void close() throws SQLException {
        closed = true;
        batch.clear();
        if (currentRs != null) currentRs.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {

    }

    @Override public boolean isClosed() { return closed; }

    /* ------------------------------------------------------------
     * Simple settings
     * ------------------------------------------------------------ */
    @Override public void setMaxRows(int max) { this.maxRows = max; }
    @Override public int  getMaxRows() { return maxRows; }
    @Override public void setQueryTimeout(int seconds) { this.queryTimeout = seconds; }
    @Override public int  getQueryTimeout() { return queryTimeout; }
    @Override public void cancel() { /* ignore for now */ }

    /* ------------------------------------------------------------
     * Unsupported or simplified methods
     * ------------------------------------------------------------ */
    @Override public void setEscapeProcessing(boolean enable) { /* ignore */ }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() { /* ignore */ }

    @Override
    public void setCursorName(String name) throws SQLException {

    }

    @Override public ResultSet getGeneratedKeys() throws SQLException { throw notSupported(); }
    @Override public int getResultSetConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public void setFetchDirection(int direction) { /* ignore */ }
    @Override public int  getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) { /* ignore */ }
    @Override public int  getFetchSize() { return 0; }
    @Override public Connection getConnection() { return parentConn; }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    /* Overloads returning same behavior as basic execute/update/query */
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return execute(sql); }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return executeUpdate(sql); }

    /* ------------------------------------------------------------
     * JDBC 4.0+ default / advanced features — 未实现
     * ------------------------------------------------------------ */
    @Override public boolean isPoolable() { return false; }
    @Override public void setPoolable(boolean poolable) { /* ignore */ }
    @Override public void closeOnCompletion() { /* ignore */ }
    @Override public boolean isCloseOnCompletion() { return false; }
    @Override public long getLargeUpdateCount() { return getUpdateCount(); }
    @Override public void setLargeMaxRows(long max) { /* ignore */ }
    @Override public long getLargeMaxRows() { return 0; }
    @Override public long[] executeLargeBatch() throws SQLException { throw notSupported(); }
    @Override public long  executeLargeUpdate(String sql) throws SQLException { throw notSupported(); }

    /* ------------------------------------------------------------
     * Wrapper
     * ------------------------------------------------------------ */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
    @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }

    /* ------------------------------------------------------------
     * Methods not listed above can be added later as needed
     * ------------------------------------------------------------ */


}
