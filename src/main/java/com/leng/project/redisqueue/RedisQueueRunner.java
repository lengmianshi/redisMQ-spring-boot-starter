package com.leng.project.redisqueue;

import com.leng.project.redisqueue.annotation.RedisDelayQueueListener;
import com.leng.project.redisqueue.annotation.RedisQueueListener;
import com.leng.project.redisqueue.handler.QueueHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

@Slf4j
public class RedisQueueRunner implements CommandLineRunner {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private QueueHandler queueHandler;
    @Autowired
    private RedisQueueTemplate redisQueueTemplate;

    @Override
    public void run(String... args) {
        //从spring的bean中扫描@RedissonDelayListener，并执行对应的方法
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean existManualAck = false;//是否需要手动确认
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(RedisDelayQueueListener.class)) {
                    RedisDelayQueueListener annotation = method.getDeclaredAnnotation(RedisDelayQueueListener.class);
                    if (!annotation.autoAck()) {
                        existManualAck = true;
                    }
                    queueHandler.registerDelayListener(annotation, bean, method);
                } else if (method.isAnnotationPresent(RedisQueueListener.class)) {
                    RedisQueueListener annotation = method.getDeclaredAnnotation(RedisQueueListener.class);
                    if (!annotation.autoAck()) {
                        existManualAck = true;
                    }
                    queueHandler.registerListener(annotation, bean, method);
                }
            }
        }

        if (existManualAck) {
            //存在手动确认的情况，启动过期待确认消息重新入队线程
            redisQueueTemplate.unAckMessageRequeue(1);
        }
    }
}
