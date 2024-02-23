package com.leng.project.redisqueue.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisQueueListener {
    /**
     * 队列名
     *
     * @return
     */
    String queue() default "";

    /**
     * 消费者线程数
     *
     * @return
     */
    int consumers() default 1;

    /**
     * 是否自动确认
     *
     * @return
     */
    boolean autoAck() default true;

    /**
     * 一次从队列中取多少数据
     *
     * @return
     */
    int prefetch() default 100;

    /**
     * 获取消息的频率，单位秒
     * @return
     */
    long frequency() default 2;
}
