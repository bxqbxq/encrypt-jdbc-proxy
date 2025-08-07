package com.seu.jdbcproxy;

import java.sql.SQLException;
import java.util.Map;

public class OperationExecutor {
    private final DatabaseOperator operator = new DatabaseOperator();

    public void execute(String operation, Map<String, String> params) {
        try {
            operator.execute(operation, params);
        } catch (SQLException e) {
            throw new RuntimeException("操作执行失败: " + e.getMessage(), e);
        }
    }
}
