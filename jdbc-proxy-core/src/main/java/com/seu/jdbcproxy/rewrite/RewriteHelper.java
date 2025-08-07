package com.seu.jdbcproxy.rewrite;


import com.seu.jdbcproxy.rewrite.SQLRewriter.SQLRewrite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewriteHelper {
    private final SQLDevNavigatorSQLRewriter rewriter = new SQLDevNavigatorSQLRewriter();
    private final List<Method> fullRewriterMethods = new ArrayList<>();
    private final List<Method> partialRewriterMethods = new ArrayList<>();

    public RewriteHelper() {
        super();
        populateRewriterMethods();
    }

    private void populateRewriterMethods() {
        for (Method method : rewriter.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SQLRewrite.class)) {
                SQLRewrite annotation = method.getAnnotation(SQLRewrite.class);
                if (annotation.full()) {
                    fullRewriterMethods.add(method);
                } else {
                    partialRewriterMethods.add(method);
                }
            }
        }
    }

    /**
     * Rewrites a sql in MySQL dialect to the target dialect (based on product).
     */
    //两个参数的rewrite方法：定义result为三参数的rewrite方法（遍历全部重写方法），返回一个sql语句，再将result作为
    //三参数rewrite（遍历部分重写方法）中的参数，调用并返回，目的是：为了全部覆盖？
    public String rewrite(String sql, String product) {
        String result = rewrite(fullRewriterMethods, sql, product);
        return rewrite(partialRewriterMethods, result, product);
    }

    /**
     * Rewrites a call statement written for Oracle Databases to a
     * generic target (independent of the product).
     */
    public String rewriteCall(String sql) {
        final Pattern p = Pattern.compile("(?i)^\\s*BEGIN\\s+(.+?)\\s*;\\s*END\\s*;\\s*$");
        final Matcher m = p.matcher(sql);
        if (m.find()) {
            return "CALL " + m.group(1)
                    .replaceAll("(?i)\\s*TO_NUMBER\\s*\\(\\s*\\?\\s*\\)\\s*", "?")
                    .replaceAll("(?i)\\s*TO_CHAR\\s*\\(\\s*\\?\\s*\\)\\s*", "?");
        }
        return sql;
    }

    private String rewrite(List<Method> methods, String sql, String product) {  //三个参数的rewrite方法：调用SQLDevNav中的每个重写方法
        String result = sql;
        for (Method method : methods) {
            try {
                result = (String) method.invoke(rewriter, result, product);
            } catch (IllegalAccessException | InvocationTargetException e ) {
                throw new RuntimeException("Cannot rewrite SQL statement for " + product + ".");
            }
        }
        return result;
    }

}
