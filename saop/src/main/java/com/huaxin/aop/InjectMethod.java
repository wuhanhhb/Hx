package com.huaxin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by hebing on 2016/4/12.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface InjectMethod {

    String[] params() default {};
}
