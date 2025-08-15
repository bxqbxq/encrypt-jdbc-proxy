package com.seu.jdbcproxy.server;

import com.seu.jdbcproxy.core.ProxyPreparedStatement;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;
import com.seu.jdbcproxy.rewrite.EncryptionHelper;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Map<String, Connection> connMap;
    private final Map<String, ResultSet> rsMap;
    private final EncryptionHelper encryptionHelper;

    public ClientHandler(Socket socket, Map<String, Connection> connMap, Map<String, ResultSet> rsMap) {
        this.socket = socket;
        this.connMap = connMap;
        this.rsMap = rsMap;
        encryptionHelper = new EncryptionHelper();
    }

    @Override
    public void run() {
        try(ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                Request req = (Request) in.readObject();
                Response resp = dispatch(req);
                out.writeObject(resp);
                out.flush();
            }
        }catch (EOFException ignored){

        }catch (Exception e){
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

                    /* 2. 把逻辑 connId 回给客户端 */
                    System.out.println("Handling OPEN_CONN request");
                    r.setType(MessageType.OPEN_CONN);
                    r.setRows(List.of(List.of(id)));
                }

                case EXEC_QUERY -> {
                    Connection c = connMap.get(q.getConnId());
                    String sql = q.getSql();
                    Map<Integer, Object> params = q.getParams();

                    PreparedStatement preparedStatement = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
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

                    rsMap.put(q.getConnId(), rs);
                    r.setType(MessageType.EXEC_QUERY);
                    r.setColumnNames(columnNames);
                    r.setRows(fetch(rs, q.getFetchSize() > 0 ? q.getFetchSize() : 100, r));
                    r.setHasMoreRows(!rs.isAfterLast());
                }


                case FETCH_ROWS -> {
                    ResultSet rs = rsMap.get(q.getConnId());
                    r.setRows(fetch(rs, q.getFetchSize(), r));
                }

                case EXEC_UPDATE -> {
                    Connection c = connMap.get(q.getConnId());
                    String sql = q.getSql();
                    Map<Integer, Object> params = q.getParams(); // 原类型

                    try (ProxyPreparedStatement stmt = new ProxyPreparedStatement(c.prepareStatement(sql), sql)) {
                        if (params != null) {
                            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                                stmt.setObject(entry.getKey(), entry.getValue());
                            }
                        }
                        int affected = stmt.executeUpdate();
                        r.setUpdateCount(affected);
                        //r.setRows(List.of(List.of(affected)));
                    }
                }
                case CLOSE_CONN -> closeConn(q.getConnId());
                case EXCEPTION -> {
                }
                case PING -> {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    private List<List<Object>> fetch(ResultSet rs, int n, Response r) throws Exception {
        List<List<Object>> rows = new ArrayList<>();
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
        return rows;
    }

    private void closeConn(String id) {
        try {
            if (connMap.containsKey(id)) {
                connMap.get(id).close();
            }
        } catch (SQLException ignored) {
        }
        rsMap.remove(id);
        connMap.remove(id);
    }
}
