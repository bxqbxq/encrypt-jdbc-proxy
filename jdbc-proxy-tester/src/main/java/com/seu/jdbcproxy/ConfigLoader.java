package com.seu.jdbcproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final Properties props = new Properties();
    private static final String DEFAULT_CONFIG_FILE = "config.properties";

    static {
        loadConfig(DEFAULT_CONFIG_FILE);
    }

    // 加载配置文件
    private static void loadConfig(String filename) {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                logger.warn("tester/配置文件 {} 未找到，使用默认配置", filename);
            } else {
                props.load(input);
                logger.info("tester/配置文件 {} 已加载", filename);
            }
        } catch (Exception e) {
            logger.error("tester/加载配置文件失败: {}", e.getMessage(), e);
        }
    }

    // 获取配置
    public static String get(String key) {
        return props.getProperty(key);
    }

    // 获取带默认值的配置
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    // 判断配置是否存在
    public static boolean containsKey(String key) {
        return props.containsKey(key);
    }

    // 支持命令行参数覆盖配置
    public static void overrideWithArgs(String[] args) {
        for (String arg : args) {
            if (arg.contains("=")) {
                String[] keyValue = arg.split("=", 2);
                props.setProperty(keyValue[0], keyValue[1]);
                logger.info("命令行参数覆盖配置: {} = {}", keyValue[0], keyValue[1]);
            }
        }
    }
}
