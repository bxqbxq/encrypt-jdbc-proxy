package com.seu.jdbcproxy;

import java.util.HashMap;
import java.util.Map;


public class TestRunner {
    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("com.seu.jdbcproxy.tester.ProxyDriver");
        ConfigLoader.overrideWithArgs(args);

        Map<String, String> params = parseArgs(args);

        if (params.containsKey("operation")) {
            new OperationExecutor().execute(params.get("operation"), params);
        } else {
            // 执行主程序功能（如果有）
            Main.main(new String[0]);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] kv = arg.split("=", 2);
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
