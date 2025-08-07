package com.seu.jdbcproxy.pojo;

public enum MessageType {
    OPEN_CONN, EXEC_QUERY, EXEC_UPDATE, FETCH_ROWS, CLOSE_CONN,
    EXCEPTION, PING
}
