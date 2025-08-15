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

public class ProxyServer {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private static final Map<String, Connection> connMap = new HashMap<>();
    private static final Map<String, ResultSet> rsMap = new HashMap<>();
    private final EncryptionHelper encryptionHelper = new EncryptionHelper();
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    public ProxyServer(int port) {
        this.port = port;
    }

//    public void start() throws IOException {
//        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            System.out.println("ProxyServer listening on port " + port);
//            while (true) {
//                pool.submit(() -> {
//                    try {
//                        handle(serverSocket.accept());
//                        logger.info("Connection accepted");
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//            }
//        }
//    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "9999"));
        //new ProxyServer(port).start();

        ExecutorService executor = Executors.newCachedThreadPool();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ProxyServer listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());

                executor.execute(new ClientHandler(socket,connMap,rsMap));
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
