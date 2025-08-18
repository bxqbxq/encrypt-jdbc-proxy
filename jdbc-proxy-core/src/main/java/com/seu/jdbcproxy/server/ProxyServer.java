package com.seu.jdbcproxy.server;

import com.seu.jdbcproxy.core.ProxyPreparedStatement;
import com.seu.jdbcproxy.pojo.MessageType;
import com.seu.jdbcproxy.pojo.Request;
import com.seu.jdbcproxy.pojo.Response;
import com.seu.jdbcproxy.rewrite.EncryptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProxyServer {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 使用ConcurrentHashMap确保线程安全
    private static final Map<String, Connection> connMap = new ConcurrentHashMap<>();
    private static final Map<String, PreparedStatement> stmtMap = new ConcurrentHashMap<>();
    private static final Map<String, ResultSet> rsMap = new ConcurrentHashMap<>();
    
    // 连接超时配置
    private static final long CONNECTION_TIMEOUT_MS = 300000; // 5分钟
    private static final long HEARTBEAT_INTERVAL_MS = 60000;  // 1分钟
    
    private final EncryptionHelper encryptionHelper = new EncryptionHelper();
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    public ProxyServer(int port) {
        this.port = port;
        // 启动连接超时检测任务
        startConnectionTimeoutTask();
    }

    private void startConnectionTimeoutTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredConnections();
            } catch (Exception e) {
                logger.error("Error during connection cleanup", e);
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void cleanupExpiredConnections() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Connection>> iterator = connMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Connection> entry = iterator.next();
            String connId = entry.getKey();
            
            // 清理过期的Statement
            cleanupExpiredStatements(connId);
            
            // 清理过期的ResultSet
            cleanupExpiredResultSets(connId);
            
            // 检查连接是否超时（这里可以添加连接最后活动时间的跟踪）
            // 暂时只清理明显无效的连接
            try {
                Connection conn = entry.getValue();
                if (conn.isClosed()) {
                    iterator.remove();
                    logger.info("Removed closed connection: {}", connId);
                }
            } catch (SQLException e) {
                iterator.remove();
                logger.info("Removed invalid connection: {}", connId);
            }
        }
    }

    private void cleanupExpiredStatements(String connId) {
        Iterator<Map.Entry<String, PreparedStatement>> iterator = stmtMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PreparedStatement> entry = iterator.next();
            String stmtId = entry.getKey();
            if (stmtId.startsWith(connId + "_")) {
                try {
                    PreparedStatement stmt = entry.getValue();
                    if (stmt.isClosed()) {
                        iterator.remove();
                        logger.debug("Removed closed statement: {}", stmtId);
                    }
                } catch (SQLException e) {
                    iterator.remove();
                    logger.debug("Removed invalid statement: {}", stmtId);
                }
            }
        }
    }

    private void cleanupExpiredResultSets(String connId) {
        Iterator<Map.Entry<String, ResultSet>> iterator = rsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ResultSet> entry = iterator.next();
            String rsId = entry.getKey();
            if (rsId.startsWith(connId + "_")) {
                try {
                    ResultSet rs = entry.getValue();
                    if (rs.isClosed()) {
                        iterator.remove();
                        logger.debug("Removed closed result set: {}", rsId);
                    }
                } catch (SQLException e) {
                    iterator.remove();
                    logger.debug("Removed invalid result set: {}", rsId);
                }
            }
        }
    }

    public void shutdown() {
        pool.shutdown();
        scheduler.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "9999"));
        ProxyServer server = new ProxyServer(port);

        ExecutorService executor = Executors.newCachedThreadPool();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ProxyServer listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());

                executor.execute(new ClientHandler(socket, connMap, stmtMap, rsMap));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.shutdown();
        }
    }
}
