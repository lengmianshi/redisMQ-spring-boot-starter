package com.leng.project.redisqueue.bean;

import lombok.Data;

@Data
public class Queue {
    //队列
    private String queue;
    //虚拟空间
    private String virtualHost;
    //队列类型：1普通队列，2延时队列
    private int queueType;
    //待消费消息数
    private long total;
    //待确认数
    private long unAcked;
}
