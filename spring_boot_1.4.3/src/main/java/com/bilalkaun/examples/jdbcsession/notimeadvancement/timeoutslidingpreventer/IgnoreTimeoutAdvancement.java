package com.bilalkaun.examples.jdbcsession.notimeadvancement.timeoutslidingpreventer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any controller method annotated with this will not advance the timeout of the session
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreTimeoutAdvancement {
}
