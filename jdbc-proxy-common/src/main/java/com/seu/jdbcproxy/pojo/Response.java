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
    private SQLException exception;
    private List<String> columnNames;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public SQLException getException() {
        return exception;
    }

    public void setException(SQLException exception) {
        this.exception = exception;
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
}
