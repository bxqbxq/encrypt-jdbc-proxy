package com.seu.jdbcproxy.server;

import com.seu.jdbcproxy.core.ProxyPreparedStatement;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;
import com.seu.jdbcproxy.pojo.SQLExceptionWrapper;
import com.seu.jdbcproxy.rewrite.EncryptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Map<String, Connection> connMap;
    private final Map<String, PreparedStatement> stmtMap;
    private final Map<String, ResultSet> rsMap;
    private final EncryptionHelper encryptionHelper;
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    // 记录连接最后活动时间
    private final Map<String, Long> lastActivityMap = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_TIMEOUT_MS = 300000; // 5分钟超时

    public ClientHandler(Socket socket, Map<String, Connection> connMap, 
                        Map<String, PreparedStatement> stmtMap, Map<String, ResultSet> rsMap) {
        this.socket = socket;
        this.connMap = connMap;
        this.stmtMap = stmtMap;
        this.rsMap = rsMap;
        this.encryptionHelper = new EncryptionHelper();
    }

    @Override
    public void run() {
        // 注意：对象流握手要求两端均先创建 ObjectOutputStream 再创建 ObjectInputStream
        try(ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                Request req = (Request) in.readObject();
                
                // 检查连接是否超时
                if (req.getConnId() != null && isConnectionExpired(req.getConnId())) {
                    Response timeoutResp = new Response();
                    timeoutResp.setType(MessageType.EXCEPTION);
                    timeoutResp.setErrorMessage("Connection expired");
                    out.writeObject(timeoutResp);
                    out.flush();
                    continue;
                }
                
                Response resp = dispatch(req);
                out.writeObject(resp);
                out.flush();
                
                // 更新连接活动时间
                if (req.getConnId() != null) {
                    lastActivityMap.put(req.getConnId(), System.currentTimeMillis());
                }
            }
        } catch (EOFException ignored) {
            // 客户端正常断开
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private boolean isConnectionExpired(String connId) {
        Long lastActivity = lastActivityMap.get(connId);
        if (lastActivity == null) return false;
        return System.currentTimeMillis() - lastActivity > HEARTBEAT_TIMEOUT_MS;
    }

    private void cleanup() {
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response dispatch(Request q) throws Exception {
        Response r = new Response();
        r.setType(q.getType());
        System.out.println("Dispatching request: " + q.getType());
        
        try {
            switch (q.getType()) {
                case OPEN_CONN -> {
                    System.out.println("Handling OPEN_CONN request");
                    Map<String, Object> extra = q.getExtra();
                    if (extra == null) extra = Map.of();

                    String url = (String) extra.get("realUrl");
                    String user = (String) extra.get("user");
                    String pwd = (String) extra.get("pwd");

                    String id = UUID.randomUUID().toString();
                    Connection c = DriverManager.getConnection(url, user, pwd);

                    connMap.put(id, c);
                    lastActivityMap.put(id, System.currentTimeMillis());

                    r.setType(MessageType.OPEN_CONN);
                    r.setRows(List.of(List.of(id)));
                }

                case EXEC_QUERY -> {
                    Connection c = connMap.get(q.getConnId());
                    if (c == null) {
                        throw new SQLException("Connection not found: " + q.getConnId(), "08003", 0);
                    }
                    
                    String sql = q.getSql();
                    Map<Integer, Object> params = q.getParams();

                    // 为Statement分配唯一ID
                    String stmtId = q.getConnId() + "_" + UUID.randomUUID().toString();
                    PreparedStatement preparedStatement = c.prepareStatement(sql, 
                        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    
                    // 存储Statement
                    stmtMap.put(stmtId, preparedStatement);
                    
                    ProxyPreparedStatement proxyPreparedStatement = new ProxyPreparedStatement(preparedStatement, sql, encryptionHelper);
                    if (params != null) {
                        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                            proxyPreparedStatement.setObject(entry.getKey(), entry.getValue());
                        }
                    }
                    
                    ResultSet rs = proxyPreparedStatement.executeQuery();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(meta.getColumnLabel(i));
                    }

                    // 为ResultSet分配唯一ID
                    String rsId = stmtId + "_" + UUID.randomUUID().toString();
                    rsMap.put(rsId, rs);
                    
                    r.setType(MessageType.EXEC_QUERY);
                    r.setColumnNames(columnNames);
                    r.setRows(fetch(rs, q.getFetchSize() > 0 ? q.getFetchSize() : 100, r));
                    r.setHasMoreRows(!rs.isAfterLast());
                    r.setStatementId(stmtId);
                    r.setResultSetId(rsId);
                }

                case FETCH_ROWS -> {
                    String rsId = q.getResultSetId();
                    if (rsId == null) {
                        throw new SQLException("ResultSet ID not provided", "07000", 0);
                    }
                    
                    ResultSet rs = rsMap.get(rsId);
                    if (rs == null) {
                        throw new SQLException("ResultSet not found: " + rsId, "07000", 0);
                    }
                    
                    r.setRows(fetch(rs, q.getFetchSize(), r));
                    r.setHasMoreRows(!rs.isAfterLast());
                }

                case EXEC_UPDATE -> {
                    Connection c = connMap.get(q.getConnId());
                    if (c == null) {
                        throw new SQLException("Connection not found: " + q.getConnId(), "08003", 0);
                    }
                    
                    String sql = q.getSql();
                    Map<Integer, Object> params = q.getParams();

                    // 为Statement分配唯一ID
                    String stmtId = q.getConnId() + "_" + UUID.randomUUID().toString();
                    PreparedStatement preparedStatement = c.prepareStatement(sql);
                    stmtMap.put(stmtId, preparedStatement);

                    try (ProxyPreparedStatement stmt = new ProxyPreparedStatement(preparedStatement, sql)) {
                        if (params != null) {
                            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                                stmt.setObject(entry.getKey(), entry.getValue());
                            }
                        }
                        int affected = stmt.executeUpdate();
                        r.setUpdateCount(affected);
                        r.setStatementId(stmtId);
                    }
                }
                
                case CLOSE_STATEMENT -> {
                    String stmtId = q.getStatementId();
                    if (stmtId != null) {
                        closeStatement(stmtId);
                    }
                    r.setType(MessageType.CLOSE_STATEMENT);
                }
                
                case CLOSE_RESULT_SET -> {
                    String rsId = q.getResultSetId();
                    if (rsId != null) {
                        closeResultSet(rsId);
                    }
                    r.setType(MessageType.CLOSE_RESULT_SET);
                }
                
                case CLOSE_CONN -> {
                    closeConn(q.getConnId());
                    r.setType(MessageType.CLOSE_CONN);
                }
                
                case EXCEPTION -> {
                    r.setType(MessageType.EXCEPTION);
                    r.setErrorMessage("Exception occurred");
                }
                
                case PING -> {
                    r.setType(MessageType.PING);
                    r.setTimestamp(System.currentTimeMillis());
                }
            }
        } catch (SQLException e) {
            // SQL异常特殊处理
            r.setType(MessageType.EXCEPTION);
            r.setSqlException(new SQLExceptionWrapper(e));
            r.setErrorMessage(e.getMessage());
            logger.error("SQL Exception occurred: {}", e.getMessage(), e);
        } catch (Exception e) {
            // 其他异常转换为SQL异常
            r.setType(MessageType.EXCEPTION);
            SQLException sqlException = new SQLException("Proxy server error: " + e.getMessage(), e);
            r.setSqlException(new SQLExceptionWrapper(sqlException));
            r.setErrorMessage(e.getMessage());
            logger.error("Unexpected exception occurred: {}", e.getMessage(), e);
        }
        return r;
    }

    private void closeStatement(String stmtId) {
        try {
            PreparedStatement stmt = stmtMap.remove(stmtId);
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            logger.warn("Error closing statement {}: {}", stmtId, e.getMessage());
        }
    }

    private void closeResultSet(String rsId) {
        try {
            ResultSet rs = rsMap.remove(rsId);
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException e) {
            logger.warn("Error closing result set {}: {}", rsId, e.getMessage());
        }
    }

    private List<List<Object>> fetch(ResultSet rs, int n, Response r) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            int i = 0;
            while (i < n && rs.next()) {
                List<Object> row = new ArrayList<>(cols);
                for (int c = 1; c <= cols; c++) {
                    row.add(rs.getObject(c));
                }
                rows.add(row);
                i++;
            }
            r.setHasMoreRows(!rs.isAfterLast());
        } catch (SQLException e) {
            logger.error("Error fetching rows from result set: {}", e.getMessage(), e);
            throw e;
        }
        return rows;
    }

    private void closeConn(String id) {
        try {
            // 清理相关的Statement和ResultSet
            cleanupConnectionResources(id);
            
            if (connMap.containsKey(id)) {
                Connection conn = connMap.get(id);
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
                connMap.remove(id);
            }
            lastActivityMap.remove(id);
            logger.info("Connection {} closed successfully", id);
        } catch (SQLException e) {
            logger.warn("Error closing connection {}: {}", id, e.getMessage());
        }
    }

    private void cleanupConnectionResources(String connId) {
        // 清理相关的Statement
        stmtMap.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(connId + "_")) {
                try {
                    if (!entry.getValue().isClosed()) {
                        entry.getValue().close();
                    }
                } catch (SQLException e) {
                    logger.warn("Error closing statement during connection cleanup: {}", e.getMessage());
                }
                return true;
            }
            return false;
        });
        
        // 清理相关的ResultSet
        rsMap.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(connId + "_")) {
                try {
                    if (!entry.getValue().isClosed()) {
                        entry.getValue().close();
                    }
                } catch (SQLException e) {
                    logger.warn("Error closing result set during connection cleanup: {}", e.getMessage());
                }
                return true;
            }
            return false;
        });
    }
}
