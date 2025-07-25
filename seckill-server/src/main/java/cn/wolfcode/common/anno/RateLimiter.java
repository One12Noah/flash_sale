package cn.wolfcode.common.anno;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    String key() default "";
    long permitsPerSecond() default 1;
    long timeout() default 500;
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}