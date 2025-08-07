package com.seu.jdbcproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtil {
    private static final Logger logger = LoggerFactory.getLogger(UrlUtil.class);
    public static final String INVALID_FORMAT = "Invalid proxy URL. Expected format: jdbc:mysql://<targetUrl>:[<port>]/[<database>]";

    private UrlUtil() {
        // do not instantiate
    }

    /**
     * Extracts the target part of a JDBC URL constructed by SQL Developer.
     * Example: "jdbc:mysql:jdbc:mysql://localhost:3306/mysql:3306/mysql"
     * returns "jdbc:mysql://localhost:3306/mysql".
     */

    public static String extractTargetUrl(String url) {   //******修改前******
        final Pattern p = Pattern.compile("^(jdbc:mysql:\\/\\/)(.+?)(:([0-9]+)?\\/([^\\/:]+)?)$");
        /*
        ^(jdbc:mysql:\/\/)用来捕获jdbc:mysql://
        .+? 非贪婪匹配字符，直到遇到后面的模式
         */
        final Matcher m = p.matcher(url);
        final boolean found = m.find();
        assert found : INVALID_FORMAT;
        return m.group(2);
    }

    /**
     * 用来提取  插入语句  的值字段
     * 但是只能作用于直接插入，不能作用于复制插入的情形
     * 例如：insert into my_tables (test) values ('hello') -> hello
     */
    public static String extractValues(String sql) {
        String regex = "INSERT INTO\\s+\\w+\\s*\\([^)]*\\)\\s+VALUES\\s*\\(([^)]+)\\)";

        // 创建正则表达式的Pattern对象
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(sql);

        //logger.info("urlutil的提取值方法");
        // 如果匹配成功，提取出VALUES后的内容
        if (matcher.find()) {
            String values = matcher.group(1).trim();

            // 去掉两端的引号（如果有）
            if (values.startsWith("'") && values.endsWith("'")) {
                values = values.substring(1, values.length() - 1); // 去除首尾引号
            }

            return values;
        } else {
            return "No match found";  // 如果没有匹配到，返回提示信息
        }
    }
}

