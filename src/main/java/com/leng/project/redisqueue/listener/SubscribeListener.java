package com.leng.project.redisqueue.listener;

import com.alibaba.fastjson.JSONObject;
import com.leng.project.redisqueue.Constant;
import com.leng.project.redisqueue.utils.MyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.lang.reflect.Method;

@Slf4j
public class SubscribeListener implements MessageListener {
    //消费端对象和消费方法
    private Object bean;
    private Method method;

    public SubscribeListener(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        Object[] params = new Object[method.getParameterTypes().length];

        if (params.length > 0) {
            Class<?> clazz = method.getParameterTypes()[0];
            Object data = JSONObject.parseObject(message.getBody(), clazz);
            params[0] = data;

            if (params.length >= 2) {
                //将第2个参数设为当前channel
                params[1] = String.valueOf(message.getChannel());
            }
        }

        //调用消费方法
        try {
            //缓存当前channel名称，当topic中存在通配符时，可能需要知道当前是哪个channel发来的消息
            MyContext.setValue(Constant.CURRENT_SUBSCRIBE_CHANNEL_KEY, new String(message.getChannel()));
            method.invoke(bean, params);
        } catch (Exception e) {
            log.error("调用订阅消费端出错：{}", e.getMessage(), e);
        } finally {
            MyContext.removeValue(Constant.CURRENT_SUBSCRIBE_CHANNEL_KEY);
        }
//        System.out.println("接收数据:" + message.toString());
//        System.out.println("订阅频道:" + new String(message.getChannel()));
    }
}
