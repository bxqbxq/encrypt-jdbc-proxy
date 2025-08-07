package com.seu.jdbcproxy;

import com.seu.jdbcproxy.rewrite.DatabaseProxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Main {
    private String implementationDate = "0000-00-00 00:00:00";
    private String implementationVersion = "x.y.z";

    Main() {
        super();
    }

    private void print(String msg) {
        System.out.println(msg);
        setManifestProperties();
    }

    private void setManifestProperties() {
        File file;
        try {
            // file in IDE: classes directory; file in runtime environment: jar file
            file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            return;
        }
        if (file.exists()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            implementationDate = dateFormat.format(file.lastModified());
            if (file.getName().endsWith(".jar")) {
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    return;
                }
                JarInputStream jis;
                try {
                    jis = new JarInputStream(fis);
                } catch (IOException e) {
                    return;
                }
                Manifest mf = jis.getManifest();
                implementationVersion = mf.getMainAttributes().getValue("Implementation-Version");
                try {
                    jis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void printInfo() {
        print("");
        print("JDBC Proxy for SQL Developer version " + implementationVersion + " built on " + implementationDate + ".");
        print("Copyright 2021 Philipp Salvisberg <philipp.salvisberg@trivadis.com>");
        print("");
    }

    public static void main(String[] args) {
// 1. 目标 SQL 语句（SELECT 查询语句）
        String selectSql = "select id,test from my_table where test = ?";

        // 2. 数据库连接 URL（主数据库）
        String dbUrl = "jdbc:mysql://localhost:3306/test_mysql";

        // 3. 插入用的 SQL（非查询语句测试）
        String insertSql = "select id,test from my_table where test = ?";

        // 4. 测试执行 SELECT 查询（这将同时触发对备用库的查询）
        System.out.println("========== 测试 SELECT 查询 ==========");
        //DatabaseProxy.proxyExecute(selectSql, dbUrl, "test try123");







//        Main instance = new Main();
//        instance.printInfo();
    }

}
