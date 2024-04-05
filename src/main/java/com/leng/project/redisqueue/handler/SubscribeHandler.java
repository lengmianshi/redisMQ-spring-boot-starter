package com.leng.project.redisqueue.handler;

import com.leng.project.redisqueue.annotation.RedisSubscribeListener;
import com.leng.project.redisqueue.listener.SubscribeListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.lang.reflect.Method;

public class SubscribeHandler {
    @Autowired
    private RedisMessageListenerContainer container;

    public void registerListener(RedisSubscribeListener annotation, Object bean, Method method) {
        SubscribeListener listener = new SubscribeListener(bean, method);

//        MessageListenerAdapter adapter = springBeanUtils.registerBean("messageListenerAdapter", MessageListenerAdapter.class, false, listener);
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener);
        //向RedisMessageListenerContainer中添加channel监听
        container.addMessageListener(adapter, new PatternTopic(annotation.channel()));
    }
}
