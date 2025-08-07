package com.seu.jdbcproxy.tester;

//import com.seu.jdbcproxy.core.ProxyConnection;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ProxyDriver implements Driver {
    /** URL 前缀 */
    public static final String PREFIX = "jdbc:proxy://";
    private static final String PROXY_SCHEME = "proxy://";
    /* ---------- 注册 ---------- */

    static {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            //System.out.println("tester----注册成功！！");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register ProxyDriver", e);
        }
    }

    /* ---------- Driver 接口实现 ---------- */

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        System.out.println("[DEBUG] ProxyDriver.connect received URL: " + url);
        return url != null && url.startsWith(PREFIX);

    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            // 不是本驱动的前缀，返回 null 让其它驱动尝试
            return null;
        }

        try {
            // 1) 拆 URL
            URI uri = parseProxyUri(url);
            String host = uri.getHost();
            int port    = (uri.getPort() == -1 ? 9999 : uri.getPort());
            Map<String, String> qs = splitQuery(uri.getRawQuery());

            // 2) 合并 Properties（覆盖同名 query 参数）
            if (info != null) {
                info.forEach((k, v) -> qs.put(k.toString(), v == null ? null : v.toString()));
            }

            String realUrl  = qs.get("realUrl");
            String user     = qs.getOrDefault("user", "");
            String password = qs.getOrDefault("password", "");
            if (realUrl == null) {
                throw new SQLException("proxy JDBC URL missing 'realUrl' parameter.");
            }

            // 3) 建立 TCP 连接
            Socket socket = new Socket(host, port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());

            // 4) 发送 OPEN_CONN
            Request open = new Request();
            open.setType(MessageType.OPEN_CONN);
            Map<String, Object> extra = new HashMap<>();
            extra.put("realUrl", realUrl);
            extra.put("user",    user);
            extra.put("pwd",     password);
            open.setExtra(extra);

            out.writeObject(open);
            out.flush();

            // 5) 读取返回
            Response resp = (Response) in.readObject();
            if (resp.getType() == MessageType.EXCEPTION) {
                throw wrap(resp.getException());
            }
            String connId = (String) resp.getRows().get(0).get(0);

            // 6) 返回 Connection 的代理实现
            return new ProxyConnection(socket, in, out, connId);
        } catch (IOException | ClassNotFoundException ex) {
            throw new SQLException("Failed to create proxy connection", ex);
        }
    }

    /* ---------- 工具方法 ---------- */

    /** 解析 proxy URL（去掉前缀再交给 URI） */
//    private URI parseProxyUri(String url) throws SQLException {
//        try {
//            return new URI(url.substring(PREFIX.length()));
//        } catch (URISyntaxException e) {
//            throw new SQLException("Malformed proxy JDBC URL: " + url, e);
//        }
//    }

    private URI parseProxyUri(String url) throws SQLException {
        try {
            String cleanUrl = url.substring(PREFIX.length());
            // 特殊处理：将查询参数中的冒号转义（关键修复！）
            cleanUrl = cleanUrl.replace("?realUrl=jdbc:", "?realUrl=jdbc%3A");

            return new URI(PROXY_SCHEME + cleanUrl); // 添加 proxy:// 前缀
        } catch (URISyntaxException e) {
            throw new SQLException("Malformed proxy JDBC URL: " + url, e);
        }
    }

    /** 把 query string 拆成 Map */
    private Map<String, String> splitQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;

        for (String kv : raw.split("&")) {
            int eq = kv.indexOf('=');
            if (eq >= 0) {
                String k = decode(kv.substring(0, eq));
                String v = decode(kv.substring(eq + 1));
                map.put(k, v);
            } else {
                map.put(decode(kv), "");
            }
        }
        return map;
    }

    private String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private SQLException wrap(SQLException e) {
        return (e != null ? e : new SQLException("Unknown error from proxy server"));
    }

    /* ---------- 其余 Driver SPI 样板 ---------- */

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        DriverPropertyInfo real = new DriverPropertyInfo("realUrl", null);
        real.required = true;
        real.description = "Actual JDBC URL of the target database.";

        DriverPropertyInfo user = new DriverPropertyInfo("user", null);
        user.description = "Database user name.";

        DriverPropertyInfo pwd  = new DriverPropertyInfo("password", null);
        pwd.description  = "Database password.";

        return new DriverPropertyInfo[] { real, user, pwd };
    }

    @Override public int  getMajorVersion()       { return 1; }
    @Override public int  getMinorVersion()       { return 0; }
    @Override public boolean jdbcCompliant()      { return false; }
    @Override public Logger getParentLogger()     { return Logger.getLogger("com.example.proxyjdbc"); }
}

