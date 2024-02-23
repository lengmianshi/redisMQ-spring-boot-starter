package com.leng.project.redisqueue.bean;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 从队列中取出的消息
 */
@Data
public class DelayMessageParam<T> {
    private long delay;
    private TimeUnit timeUnit;
    //消息
    private T data;

    public DelayMessageParam(T data, long delay, TimeUnit timeUnit) {
        this.data = data;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }
}
