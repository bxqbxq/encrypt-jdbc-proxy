
package com.seu.jdbcproxy;

import com.seu.jdbcproxy.rewrite.RewriteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RewriteHelperTests {
    private static final Logger logger = LoggerFactory.getLogger(RewriteHelperTests.class);

    @Nested
    class WhenRewrite {
        @Test
        public void replace_backtick_for_Snowflake() {
            RewriteHelper helper = new RewriteHelper();
            Assertions.assertEquals("select \"Test\"", helper.rewrite("select `Test`", "Snowflake"));
        }

        @Test
        public void dont_replace_backtick_for_SQLite() {
            RewriteHelper helper = new RewriteHelper();
            Assertions.assertEquals("select `Test`", helper.rewrite("select `Test`", "SQLite"));
        }

        @Test
        public void replace_show_databases_for_Snowflake() {
            RewriteHelper helper = new RewriteHelper();
            logger.info(helper.rewrite("show databases", "Snowflake"));
            Assertions.assertTrue(helper.rewrite("show databases", "Snowflake").startsWith("SELECT database_name"));
        }

        @Test
        public void replace_show_databases_for_postgresql() {
            RewriteHelper helper = new RewriteHelper();
            logger.info(helper.rewrite("show databases", "PostgreSQL"));
        }

        @Test
        public void replace_show_tables_for_postgresql(){
            RewriteHelper helper = new RewriteHelper();
            String originalsql = "select TABLE_NAME from information_schema.Tables where cast(TABLE_SCHEMA as binary) = ?  \n" +
                    "\t\t\t\t\t\t\t\tand (TABLE_TYPE = 'BASE TABLE' OR table_schema='information_schema')\n" +
                    "                        ";
            String resql = helper.rewrite(originalsql,"PostgreSQL");
            logger.info(resql);
        }
    }

    @Nested
    class WhenRewriteCall {
        @Test void remove_to_number_call() {
            RewriteHelper helper = new RewriteHelper();
            Assertions.assertEquals("CALL p(?)", helper.rewriteCall(" begin  p(  to_number  (  ?  )  )  ;  end ; "));
        }

        @Test void remove_to_char_call() {
            RewriteHelper helper = new RewriteHelper();
            Assertions.assertEquals("CALL p(?)", helper.rewriteCall("begin p(to_char(?));end;"));
        }
    }
}
