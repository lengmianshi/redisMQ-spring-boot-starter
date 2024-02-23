package com.leng.project.redisqueue.bean;

import lombok.Data;

/**
 * 从队列中取出的消息
 */
@Data
public class Message<T> {
    //消息id
    private String id;

    //消息
    private T data;
    //所属队列
    private String queue;
    //队列类型
    private Integer type;

    public Message() {
    }

}
