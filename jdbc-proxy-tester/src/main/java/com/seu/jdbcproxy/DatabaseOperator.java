package com.seu.jdbcproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseOperator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseOperator.class);

    /* ------------------------------------------------------------
     * Public dispatcher
     * ------------------------------------------------------------ */
    public void execute(String operation, Map<String, String> params) throws SQLException {
        switch (operation.toLowerCase()) {
            case "insert" -> executeInsert(params);
            case "update" -> executeUpdate(params);
            case "delete" -> executeDelete(params);
            case "select" -> executeSelect(params);
            case "equivalent_select" -> executeEquivalentSelect(params);
            default -> throw new IllegalArgumentException("不支持的 operation 类型: " + operation);
        }
    }

    /* ------------------------------------------------------------
     * SELECT *
     * ------------------------------------------------------------ */
    private void executeSelect(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String columns = ConfigLoader.get("select.columns", "");

        String sql = "SELECT " + columns + " FROM " + table;
        logger.info("[查询] SQL: {}", sql);

        // 通过拼接注释执行额外查询
        if ("true".equalsIgnoreCase(params.get("extra.enabled"))){
            String extraTable = params.getOrDefault("extra.table","");
            String extraColumns = params.getOrDefault("extra.columns","");
            String extraDbUrl = params.getOrDefault("extra.db.url","");

            String extrasql = "SELECT " + extraColumns + " FROM " + extraTable;
            StringBuilder annotation = new StringBuilder();
            annotation.append(" --@extra.enabled=true");

            if (!extraTable.isEmpty()) {
                annotation.append(" --@extra.table=").append(extraTable);
            }
            if (!extraColumns.isEmpty()) {
                annotation.append(" --@extra.columns=").append(extraColumns);
            }
            if (!extraDbUrl.isEmpty()) {
                annotation.append(" --@extra.db.url=").append(extraDbUrl);
            }

            sql += annotation.toString();
            logger.info("额外查询的SQL: {}", sql);
        }

        //主查询的sql执行
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("开始遍历结果集");
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (String col : columns.split(",")) {
                    String c = col.trim();
                    sb.append(c).append("=").append(rs.getString(c)).append(" ");
                }
                logger.info("[查询结果] {}", sb.toString());


            }
        }
    }

    /* ------------------------------------------------------------
     * INSERT
     * ------------------------------------------------------------ */
    private void executeInsert(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String targetColumn = ConfigLoader.get("insert.column", "test");
        String value = ConfigLoader.get("insert.value", "test");
        
        // 优先使用命令行参数
        if (params.containsKey("insert.value")) {
            value = params.get("insert.value");
        }

        String sql = "INSERT INTO " + table + " (" + targetColumn + ") VALUES (?)";
        logger.info("[Insert] SQL: {}", sql);
        logger.info("[Insert] 插入值: {}", value);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, value);   // 明文参数
            int rows = stmt.executeUpdate();
            logger.info("[Insert] 影响行数: {}", rows);
        }
    }

    /* ------------------------------------------------------------
     * UPDATE
     * ------------------------------------------------------------ */
    private void executeUpdate(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String condColumn = ConfigLoader.get("update.condition_column", "id");
        String targetColumn = ConfigLoader.get("update.target_column", "test");
        String newValue = ConfigLoader.get("update.value", "default_value");
        String condValue = ConfigLoader.get("update.condition_value", "1");
        
        // 优先使用命令行参数
        if (params.containsKey("update.value")) {
            newValue = params.get("update.value");
        }
        if (params.containsKey("update.condition_value")) {
            condValue = params.get("update.condition_value");
        }

        String sql = "UPDATE " + table + " SET " + targetColumn + " = ? WHERE " + condColumn + " = ?";
        logger.info("[Update] SQL: {}", sql);
        logger.info("[Update] 新值: {}, 条件值: {}", newValue, condValue);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newValue);
            stmt.setString(2, condValue);
            int rows = stmt.executeUpdate();
            logger.info("[Update] 影响行数: {}", rows);
        }
    }

    /* ------------------------------------------------------------
     * DELETE
     * ------------------------------------------------------------ */
    private void executeDelete(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String condColumn = ConfigLoader.get("delete.condition_column", "test");
        String condValue = ConfigLoader.get("delete.condition_value", "default_value");
        
        // 优先使用命令行参数
        if (params.containsKey("delete.condition_value")) {
            condValue = params.get("delete.condition_value");
        }

        String sql = "DELETE FROM " + table + " WHERE " + condColumn + " = ?";
        logger.info("[Delete] SQL: {}", sql);
        logger.info("[Delete] 条件值: {}", condValue);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, condValue);
            int rows = stmt.executeUpdate();
            logger.info("[Delete] 影响行数: {}", rows);
        }
    }

    /* ------------------------------------------------------------
     * Equivalent SELECT (简单等值查询)
     * ------------------------------------------------------------ */
    private void executeEquivalentSelect(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String condColumn = ConfigLoader.get("equivalent_select.condition_column", "test");
        String columns = ConfigLoader.get("select.columns", "id,test"); // 使用select.columns作为默认值
        String searchValue = ConfigLoader.get("search.value", "default_value");
        
        // 优先使用命令行参数
        if (params.containsKey("search.value")) {
            searchValue = params.get("search.value");
        }
        if (params.containsKey("select.columns")) {
            columns = params.get("select.columns");
        }
        
        // 如果columns为空，使用默认值
        if (columns == null || columns.trim().isEmpty()) {
            columns = "id,test";
        }

        String sql = "SELECT " + columns + " FROM " + table + " WHERE " + condColumn + " = ?";
        logger.info("[等效查询] SQL: {}", sql);
        logger.info("[等效查询] 搜索值: {}", searchValue);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, searchValue);
            logger.info("[等效查询] 参数已设置: 1 = {}", searchValue);
            logger.info("[等效查询sql语句] :  {}", sql);
            /*
             //主查询的sql执行
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("开始遍历结果集");
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (String col : columns.split(",")) {
                    String c = col.trim();
                    sb.append(c).append("=").append(rs.getString(c)).append(" ");
                }
                logger.info("[查询结果] {}", sb.toString());
             */

            try (ResultSet rs = stmt.executeQuery()) {
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    StringBuilder sb = new StringBuilder();
                    for (String col : columns.split(",")) {
                        String c = col.trim();
                        sb.append(c).append("=").append(rs.getString(c)).append(" ");
                    }
                    logger.info("[查询结果] 第{}行: {}", rowCount, sb.toString());
                }
                logger.info("[等效查询] 总共返回 {} 行数据", rowCount);
            }
        }
    }
}
