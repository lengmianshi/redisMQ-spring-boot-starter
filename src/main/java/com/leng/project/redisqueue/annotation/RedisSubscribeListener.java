package com.leng.project.redisqueue.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisSubscribeListener {
    /**
     * 要订阅的频道，支持通配符，如order_*
     *
     * @return
     */
    String channel() default "";

}
