package com.leng.project.redisqueue.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisDelayQueueListener {
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

    /**
     * 失败重试时最多执行的次数，至少为1
     *
     * @return
     */
    int maxAttempts() default 1;

    /**
     * 失败重试时间隔时长，毫秒，必须大于0
     *
     * @return
     */
    long backOffPeriod() default 3000;

    /**
     * 消费失败时是否重新入队
     * @return
     */
    boolean requeue() default false;
}
