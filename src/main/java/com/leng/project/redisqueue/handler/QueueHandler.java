package com.leng.project.redisqueue.handler;

import com.leng.project.redisqueue.Constant;
import com.leng.project.redisqueue.RedisQueueTemplate;
import com.leng.project.redisqueue.annotation.RedisDelayQueueListener;
import com.leng.project.redisqueue.annotation.RedisQueueListener;
import com.leng.project.redisqueue.bean.Annotation;
import com.leng.project.redisqueue.bean.Message;
import com.leng.project.redisqueue.exception.AckException;
import com.leng.project.redisqueue.exception.ConsumeException;
import com.leng.project.redisqueue.utils.RetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.support.RetryTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class QueueHandler {
    @Autowired
    private RedisQueueTemplate redisQueueTemplate;
    private RetryTemplate retryTemplate = RetryUtils.getTemplate(3, 300);


    /**
     * 注册延时队列监听器
     *
     * @param annotation 注解
     * @param bean       spring bean
     * @param method     被注解的方法
     */
    public void registerDelayListener(RedisDelayQueueListener annotation, Object bean, Method method) {
        Annotation an = new Annotation();
        an.setQueue(annotation.queue());
        an.setConsumers(annotation.consumers());
        an.setAutoAck(annotation.autoAck());
        an.setPrefetch(annotation.prefetch());
        an.setFrequency(annotation.frequency());
        an.setClazz(method.getParameterTypes()[0]);
        an.setQueueType(Constant.QueueType.ZSET);
        an.setMaxAttempts(annotation.maxAttempts());
        an.setBackOffPeriod(annotation.backOffPeriod());
        an.setRetryTemplate(RetryUtils.getTemplate(annotation.maxAttempts(), annotation.backOffPeriod()));

        this.registerListener0(an, bean, method,
                () -> redisQueueTemplate.takeDelayMessage(an)
        );
    }


    /**
     * 注册队列监听器
     *
     * @param annotation 注解
     * @param bean       spring bean
     * @param method     被注解的方法
     */
    public void registerListener(RedisQueueListener annotation, Object bean, Method method) {
        Annotation an = new Annotation();
        an.setQueue(annotation.queue());
        an.setConsumers(annotation.consumers());
        an.setAutoAck(annotation.autoAck());
        an.setPrefetch(annotation.prefetch());
        an.setFrequency(annotation.frequency());
        an.setClazz(method.getParameterTypes()[0]);
        an.setQueueType(Constant.QueueType.LIST);
        an.setMaxAttempts(annotation.maxAttempts());
        an.setBackOffPeriod(annotation.backOffPeriod());
        an.setRetryTemplate(RetryUtils.getTemplate(annotation.maxAttempts(), annotation.backOffPeriod()));

        this.registerListener0(an, bean, method,
                () -> redisQueueTemplate.takeMessage(an)
        );
    }


    /**
     * 注册队列监听器基础方法
     *
     * @param annotation
     * @param bean
     * @param method
     * @param supplier   获取队列消息的函数
     */
    private void registerListener0(Annotation annotation, Object bean, Method method, Supplier<List<Message<?>>> supplier) {
        //注册队列
        redisQueueTemplate.registerQueue(annotation.getQueue(), annotation.getQueueType());

        //消费者个数
        for (int i = 1; i <= Math.max(1, annotation.getConsumers()); i++) {
            new Thread(() -> {
                while (true) {
                    List<Message<?>> messages = null;
                    try {
                        //从队列中获取消息
                        messages = supplier.get();

                        if (log.isDebugEnabled()) {
                            log.debug("当前消费线程：queue={}, id={}", annotation.getQueue(), Thread.currentThread().getId());
                        }

                        //调用消费方法
                        for (Message message : messages) {
                            try {
                                annotation.getRetryTemplate().execute(
                                        //重试执行的方法
                                        (context) -> method.invoke(bean, message.getData()),
                                        //多次重试后依然失败的回调
                                        (context) -> {
                                            Throwable t = context.getLastThrowable();
                                            //抛出异常
                                            throw new ConsumeException(t);
                                        });
                                if (!annotation.isAutoAck()) {
                                    //手动确认消息
                                    //如果消息被消费了，但确认失败或是还没来得及确认进程就结束了，待确认的消息会重新入队，这时消费端需自行判断处理，避免重复消费
                                    retryTemplate.execute(
                                            (context -> {
                                                redisQueueTemplate.ack(message);
                                                return null;
                                            }),
                                            (context -> {
                                                Throwable t = context.getLastThrowable();
                                                //抛出异常
                                                throw new AckException(t);
                                            }));

                                }
                            } catch (ConsumeException e) {
                                log.error("队列消费出错：id={}, queue={}, data={}", message.getId(), annotation.getQueue(), message.getData(), e);
                                //重新入队
                                if (annotation.isRequeue()) {
                                    retryTemplate.execute((ctx -> {
                                        redisQueueTemplate.requeue(message);
                                        return null;
                                    }));
                                }
                            } catch (AckException e) {
                                log.error("队列确认出错：id={}, queue={}, data={}", message.getId(), annotation.getQueue(), message.getData(), e);
                            } catch (Exception e) {
                                log.error("", e);
                            }

                        }

                    } catch (RedisConnectionFailureException e) {
                        //一般是进程退出时报出的异常
                        log.warn(e.getMessage());
                    } catch (Exception e) {
                        log.error("队列出错：queue={}", annotation.getQueue(), e);
                    }
                }
            }).start();
        }

    }
}
