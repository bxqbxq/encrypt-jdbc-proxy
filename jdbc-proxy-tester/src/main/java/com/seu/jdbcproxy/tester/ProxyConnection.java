package com.seu.jdbcproxy.tester;

//import com.seu.jdbcproxy.core.ProxyPreparedStatement;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;


public class ProxyConnection implements Connection {

    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final String connId;


    // State flags (本地缓存)

    private volatile boolean closed = false;
    private boolean autoCommit = true;
    private boolean readOnly   = false;
    private String  catalog;
    private int transactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
    private static final Logger logger = LoggerFactory.getLogger(com.seu.jdbcproxy.core.ProxyConnection.class);

    /* ------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------ */
    public ProxyConnection(Socket socket,
                              ObjectInputStream in,
                              ObjectOutputStream out,
                              String connId) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.connId = connId;
    }

    /* ------------------------------------------------------------
     * Internal helpers
     * ------------------------------------------------------------ */
    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("Connection already closed");
    }

    private Response send(Request req) throws SQLException {
        try {
            req.setConnId(connId);
            out.writeObject(req);
            out.flush();
            Response resp = (Response) in.readObject();

            if (resp.getType() == MessageType.EXCEPTION) {
                SQLException e = resp.getException();
                throw (e != null ? e : new SQLException("Unknown server error"));
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
     * Basic Statement APIs
     * ------------------------------------------------------------ */
    @Override
    public Statement createStatement() throws SQLException {
        ensureOpen();
        return new ProxyStatement(in, out, connId, this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        ensureOpen();
        return new ProxyPreparedStatement(in, out, connId, sql, this);
    }

    /* ------------------------------------------------------------
     * CallableStatement — 目前不支持存储过程
     * ------------------------------------------------------------ */
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException { throw notSupported(); }
    @Override
    public CallableStatement prepareCall(String sql, int rsType, int rsConcurrency) throws SQLException { throw notSupported(); }
    @Override
    public CallableStatement prepareCall(String sql, int rsType, int rsConcurrency, int hold) throws SQLException { throw notSupported(); }

    /* ------------------------------------------------------------
     * Transaction / Autocommit
     * ------------------------------------------------------------ */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        ensureOpen();
        this.autoCommit = autoCommit;
        // 如需服务端事务，发送 SET_AUTOCOMMIT 指令
    }

    @Override public boolean getAutoCommit() { return autoCommit; }

    @Override public void commit()   throws SQLException { throw notSupported(); }
    @Override public void rollback() throws SQLException { throw notSupported(); }
    @Override public void rollback(Savepoint sp) throws SQLException { throw notSupported(); }

    /* ------------------------------------------------------------
     * Close / isClosed
     * ------------------------------------------------------------ */
    @Override
    public void close() throws SQLException {
        if (closed) return;

        Request q = new Request();
        q.setType(MessageType.CLOSE_CONN);
        send(q);

        try { socket.close(); } catch (IOException ignored) {}
        closed = true;
        logger.info("Proxy connection " + connId + " closed.");
    }

    @Override public boolean isClosed() { return closed; }

    /* ------------------------------------------------------------
     * Simple metadata setters / getters
     * ------------------------------------------------------------ */
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        ensureOpen();
        this.readOnly = readOnly;
    }
    @Override public boolean isReadOnly() { return readOnly; }

    @Override
    public void setCatalog(String catalog) throws SQLException { this.catalog = catalog; }
    @Override public String getCatalog() { return catalog; }

    @Override
    public void setTransactionIsolation(int level) throws SQLException { this.transactionIsolation = level; }
    @Override public int  getTransactionIsolation() { return transactionIsolation; }

    @Override public DatabaseMetaData getMetaData() throws SQLException { throw notSupported(); }

    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() { /* no‑op */ }

    /* ------------------------------------------------------------
     * Holdability / Savepoint (not yet)
     * ------------------------------------------------------------ */
    @Override public void setHoldability(int holdability) throws SQLException { throw notSupported(); }
    @Override public int  getHoldability() { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public Savepoint setSavepoint() throws SQLException { throw notSupported(); }
    @Override public Savepoint setSavepoint(String name) throws SQLException { throw notSupported(); }
    @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { throw notSupported(); }

    /* ------------------------------------------------------------
     * createStatement / prepareStatement overloads — 直接复用最简实现或抛 unsupported
     * ------------------------------------------------------------ */
    @Override public Statement createStatement(int a, int b) throws SQLException { return createStatement(); }
    @Override public Statement createStatement(int a, int b, int c) throws SQLException { return createStatement(); }

    @Override public PreparedStatement prepareStatement(String sql, int autoGenKeys) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int a, int b) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int a, int b, int c) throws SQLException { return prepareStatement(sql); }

    /* ------------------------------------------------------------
     * ClientInfo / Networking
     * ------------------------------------------------------------ */
    @Override public void setClientInfo(String name, String value) { /* ignore */ }
    @Override public void setClientInfo(Properties props) { /* ignore */ }
    @Override public String getClientInfo(String name) { return null; }
    @Override public Properties getClientInfo() { return new Properties(); }

    @Override public boolean isValid(int timeout) { return !closed; }
    @Override public void setNetworkTimeout(Executor executor, int ms) { /* ignore */ }
    @Override public int  getNetworkTimeout() { return 0; }
    @Override public void abort(Executor executor) { /* ignore */ }

    /* ------------------------------------------------------------
     * Advanced types (not supported for now)
     * ------------------------------------------------------------ */
    @Override public Clob createClob()   throws SQLException { throw notSupported(); }
    @Override public Blob createBlob()   throws SQLException { throw notSupported(); }
    @Override public NClob createNClob() throws SQLException { throw notSupported(); }
    @Override public SQLXML createSQLXML() throws SQLException { throw notSupported(); }
    @Override public Array createArrayOf(String type, Object[] arr) throws SQLException { throw notSupported(); }
    @Override public Struct createStruct(String type, Object[] attrs) throws SQLException { throw notSupported(); }
    @Override public void setSchema(String schema) throws SQLException { /* ignore */ }
    @Override public String getSchema() { return null; }
    @Override public Map<String, Class<?>> getTypeMap() { return null; }
    @Override public void setTypeMap(Map<String, Class<?>> map) { /* ignore */ }

    /* ------------------------------------------------------------
     * Wrapper
     * ------------------------------------------------------------ */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
    @Override
    public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }

    /* ------------------------------------------------------------
     * nativeSQL
     * ------------------------------------------------------------ */
    @Override public String nativeSQL(String sql) throws SQLException { return sql; }
}
