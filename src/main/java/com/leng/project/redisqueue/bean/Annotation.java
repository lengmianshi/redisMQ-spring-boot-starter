package com.leng.project.redisqueue.bean;

import lombok.Data;

@Data
public class Annotation <T>{
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
     * @return
     */
    private long frequency;

    //消息class类型
    private Class<T> clazz;

    //队列类型
    private int queueType;
}
