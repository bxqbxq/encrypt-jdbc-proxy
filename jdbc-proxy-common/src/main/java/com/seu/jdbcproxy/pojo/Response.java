package com.seu.jdbcproxy.pojo;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private List<List<Object>> rows = Collections.emptyList();
    private int updateCount = 0;
    private boolean hasMoreRows = false;
    private SQLExceptionWrapper sqlException;  // 使用包装器替代原始异常
    private List<String> columnNames;
    
    // 新增字段
    private String statementId;      // Statement ID
    private String resultSetId;      // ResultSet ID
    private long timestamp;          // 时间戳（用于心跳）
    private String errorMessage;     // 错误消息（兼容性保留）

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public SQLExceptionWrapper getSqlException() {
        return sqlException;
    }

    public void setSqlException(SQLExceptionWrapper sqlException) {
        this.sqlException = sqlException;
    }
    
    // 兼容性方法
    public SQLException getException() {
        return sqlException != null ? sqlException.toSQLException() : null;
    }

    public void setException(SQLException exception) {
        this.sqlException = exception != null ? new SQLExceptionWrapper(exception) : null;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public boolean isHasMoreRows() {
        return hasMoreRows;
    }

    public void setHasMoreRows(boolean hasMoreRows) {
        this.hasMoreRows = hasMoreRows;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId(String resultSetId) {
        this.resultSetId = resultSetId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
