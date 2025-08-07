package com.seu.jdbcproxy.rewrite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface for method annotation @SQLRewrite.
 */
public interface SQLRewriter {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface SQLRewrite {
        boolean full() default true;
    }

}
