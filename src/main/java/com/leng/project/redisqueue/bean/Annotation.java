package com.leng.project.redisqueue.bean;

import lombok.Data;
import org.springframework.retry.support.RetryTemplate;

@Data
public class Annotation<T> {
    /**
     * 队列名
     *
     * @return
     */
    private String queue;

    /**
     * 消费者线程数
     *
     * @return
     */
    private int consumers;

    /**
     * 是否自动确认
     *
     * @return
     */
    private boolean autoAck;

    /**
     * 一次从队列中取多少数据
     *
     * @return
     */
    private int prefetch;

    /**
     * 获取消息的频率，单位秒
     *
     * @return
     */
    private long frequency;

    /**
     * 失败重试时最多执行的次数，至少为1
     */
    private int maxAttempts = 1;

    /**
     * 失败重试时的间隔，毫秒
     */
    private long backOffPeriod = 3000;

    /**
     * 消费失败时是否重新入队
     */
    private boolean requeue;

    //消息class类型
    private Class<T> clazz;

    //队列类型
    private int queueType;

    private RetryTemplate retryTemplate;
}
