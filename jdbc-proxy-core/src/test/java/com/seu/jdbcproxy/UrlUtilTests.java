package com.seu.jdbcproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlUtilTests {
    private static final Logger logger = LoggerFactory.getLogger(UrlUtilTests.class);

//    @Test
//    public void mysql_proxy_with_port_and_db() {
//        String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:mysql://localhost:3306/mysql:3306/mysql");
//        logger.info(actual);
//        Assertions.assertEquals("jdbc:mysql://localhost:3306/mysql", actual);
//    }
//
//    @Test
//    public void mysql_proxy_without_port_and_with_db() {
//        String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:mysql://localhost/mysql:/mysql");
//        logger.info(actual);
//        Assertions.assertEquals("jdbc:mysql://localhost/mysql", actual);
//    }
//
//    @Test
//    public void mysql_proxy_without_port_but_with_dot_and_with_db() {
//        String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:mysql://localhost/mysql:/mysql");
//        logger.info(actual);
//        Assertions.assertEquals("jdbc:mysql://localhost/mysql", actual);
//    }
//
//    @Test
//    public void mysql_proxy_without_db_leads_to_invalid_target() {
//        //String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:mysql://localhost:3306/mysql");
//        String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:mysql://localhost:3306/company_db:3306/com");
//        logger.info(actual);
//        //Assertions.assertEquals("jdbc:mysql://localhost", actual);
//    }
//
//    @Test
//    public void mysql_postgres(){
//        //String actual = UrlUtil.extractTargetUrl("jdbc:mysql:jdbc:postgresql://localhost:5432/postgres");
//        //String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:postgres://localhost:5432/postgres");
//        String actual = UrlUtil.extractTargetUrl("jdbc:mysql://jdbc:sqlite:D:/SQLite/test.db:3306/mysql");
//        logger.info(actual);
//
//    }
//    @Test
//    public void wrong_url() {
//        AssertionError error = Assertions.assertThrows(AssertionError.class,
//                () -> UrlUtil.extractTargetUrl("jdbc:mysql2://jdbc:mysql://localhost:3306/mysql:3306:/mysql"));
//        logger.error(error.getMessage());
//        Assertions.assertEquals(UrlUtil.INVALID_FORMAT, error.getMessage());
//    }
//
//    @Test
//    public void value_extract_test() throws Exception {
//        String sql = "insert into my_table (test) values ('11/13/22点')";   //英文单引号
//        logger.info("extract_in_sql = {}",UrlUtil.extractValues(sql));
//
//        String ensql = "insert into my_table (test) values " + "('" + Encrypt.encrypt(UrlUtil.extractValues(sql)) + "')";
//        logger.info("ensql = {}",ensql);
//
////        String extract_enstr = UrlUtil.extractValues(enstr);
////        logger.info(extract_enstr);
//        logger.info("extract_in_ensql = {}",UrlUtil.extractValues(ensql));
//
//
//        String desql = "insert into my_table (test) values " + "(" + Decrypt.decrypt(UrlUtil.extractValues(ensql)) + ")";
//        logger.info("desql = {}",desql);
//        logger.info("extract_in_desql = {}",UrlUtil.extractValues(desql));
//    }

}
