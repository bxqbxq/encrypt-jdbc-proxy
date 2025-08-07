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

            String extrasql = "SELECT " + extraColumns + " FROM " + extraTable;
            StringBuilder annotation = new StringBuilder();
            annotation.append(" --@extra.enabled=true");

            if (!extraTable.isEmpty()) {
                annotation.append(" --@extra.table=").append(extraTable);
            }
            if (!extraColumns.isEmpty()) {
                annotation.append(" --@extra.columns=").append(extraColumns);
            }

            sql += annotation.toString();
            logger.info("额外查询的SQL: {}", sql);
        }

        //主查询的sql执行
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

//            ResultSetMetaData meta = rs.getMetaData();
//            List<String> realColumns = new ArrayList<>();
//            for (int i = 1; i <= meta.getColumnCount(); i++) {
//                realColumns.add(meta.getColumnLabel(i));
//            }

            System.out.println("开始遍历结果集");
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (String col : columns.split(",")) {
                    String c = col.trim();
                    sb.append(c).append("=").append(rs.getString(c)).append(" ");
                }
                logger.info("[查询结果] {}", sb.toString());


//                int id = rs.getInt(1);       //根据不同的查询字段进行修改
//                String test = rs.getString(2);
//                System.out.println("ID: " + id + "  Test: " + test); ;

            }
        }
    }

    /* ------------------------------------------------------------
     * INSERT
     * ------------------------------------------------------------ */
    private void executeInsert(Map<String, String> params) throws SQLException {
        String table = ConfigLoader.get("table.name", "my_table");
        String targetColumn = ConfigLoader.get("insert.column", "test");
//        String value = params.getOrDefault("insert.value",
//                ConfigLoader.get("insert.value", "default_value"));
        String value = ConfigLoader.get("insert.value", "test");

        String sql = "INSERT INTO " + table + " (" + targetColumn + ") VALUES (?)";
        logger.info("[Insert] SQL: {}", sql);

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
//        String newValue = params.getOrDefault("update.value",
//                ConfigLoader.get("update.alue", "new_value"));
//        String condValue = params.getOrDefault("update.condition_value",
//                ConfigLoader.get("update.condition.value", "1"));
        String newValue = ConfigLoader.get("update.value", "default_value");
        String condValue = ConfigLoader.get("update.condition_value", "1");

        String sql = "UPDATE " + table + " SET " + targetColumn + " = ? WHERE " + condColumn + " = ?";
        logger.info("[Update] SQL: {}", sql);

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
//        String condValue = params.getOrDefault("condition.value",
//                ConfigLoader.get("delete.condition.value", "1"));
        String condValue = ConfigLoader.get("delete.condition_value", "default_value");

        String sql = "DELETE FROM " + table + " WHERE " + condColumn + " = ?";
        logger.info("[Delete] SQL: {}", sql);

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
        String columns = ConfigLoader.get("columns", "");
//        String searchValue = params.getOrDefault("search_value",
//                ConfigLoader.get("query.search_value", "default_value"));
        String searchValue = ConfigLoader.get("search.value", "default_value");

        String sql = "SELECT " + columns + " FROM " + table + " WHERE " + condColumn + " = ?";
        logger.info("[等效查询] SQL: {}", sql);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, searchValue);

            try (ResultSet rs = stmt.executeQuery()) {

//                ResultSetMetaData meta = rs.getMetaData();
//                List<String> realColumns = new ArrayList<>();
//                for (int i = 1; i <= meta.getColumnCount(); i++) {
//                    realColumns.add(meta.getColumnName(i));
//                }

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
    }
}
