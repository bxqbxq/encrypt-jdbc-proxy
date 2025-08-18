package com.seu.jdbcproxy.pojo;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * SQLException包装类，用于在客户端和服务器之间传输异常信息
 */
public class SQLExceptionWrapper implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String message;
    private String sqlState;
    private int errorCode;
    private String stackTrace;
    private String causeMessage;
    
    public SQLExceptionWrapper() {}
    
    public SQLExceptionWrapper(SQLException e) {
        this.message = e.getMessage();
        this.sqlState = e.getSQLState();
        this.errorCode = e.getErrorCode();
        this.stackTrace = getStackTraceString(e);
        
        // 获取根本原因
        Throwable cause = e.getCause();
        if (cause != null) {
            this.causeMessage = cause.getMessage();
        }
    }
    
    /**
     * 将包装的异常转换回SQLException
     */
    public SQLException toSQLException() {
        SQLException sqlException = new SQLException(message, sqlState, errorCode);
        
        // 设置根本原因
        if (causeMessage != null) {
            sqlException.initCause(new RuntimeException(causeMessage));
        }
        
        return sqlException;
    }
    
    /**
     * 获取异常的堆栈跟踪字符串
     */
    private String getStackTraceString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = e.getStackTrace();
        
        // 只保留前10行堆栈信息，避免传输过多数据
        int maxLines = Math.min(10, elements.length);
        for (int i = 0; i < maxLines; i++) {
            sb.append(elements[i].toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSqlState() {
        return sqlState;
    }
    
    public void setSqlState(String sqlState) {
        this.sqlState = sqlState;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public String getCauseMessage() {
        return causeMessage;
    }
    
    public void setCauseMessage(String causeMessage) {
        this.causeMessage = causeMessage;
    }
    
    @Override
    public String toString() {
        return "SQLExceptionWrapper{" +
                "message='" + message + '\'' +
                ", sqlState='" + sqlState + '\'' +
                ", errorCode=" + errorCode +
                ", causeMessage='" + causeMessage + '\'' +
                '}';
    }
}
