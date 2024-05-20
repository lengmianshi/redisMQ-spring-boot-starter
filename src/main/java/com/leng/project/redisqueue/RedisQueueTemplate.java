package com.leng.project.redisqueue;

import com.alibaba.fastjson.JSONObject;
import com.leng.project.redisqueue.annotation.RedisSubscribeListener;
import com.leng.project.redisqueue.bean.Annotation;
import com.leng.project.redisqueue.bean.DelayMessageParam;
import com.leng.project.redisqueue.bean.Message;
import com.leng.project.redisqueue.exception.RedisDistributedLockException;
import com.leng.project.redisqueue.lock.RedisDistributedLock;
import com.leng.project.redisqueue.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Slf4j
public class RedisQueueTemplate {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private RedisDistributedLock distributedLock;

    //所有虚拟空间下的队列
    private Map<String, Integer> allQueueMap = null;

    @PostConstruct
    private void init() {
        allQueueMap = new HashMap<>();
        //获取所有虚拟空间下的队列
        Set<String> members = redisTemplate.opsForSet().members(Constant.getAllQueueKey());
        if (members == null || members.isEmpty()) {
            return;
        }

        members.forEach(e -> {
            String[] array = e.split(";");
            allQueueMap.put(array[0] + ";" + array[1], Integer.valueOf(array[2]));
        });
    }

    @RedisSubscribeListener(channel = Constant.REFRESH_ALL_QUEUE_CACHE_CHANNEL)
    public void refreshAllQueueMap() {
        this.init();
    }


    /**
     * 向延迟队列发送消息
     *
     * @param queue    队列名
     * @param message  消息
     * @param delay    延迟时间
     * @param timeUnit 时间单位
     */
    public <T> void sendDelayMessage(String queue, T message, long delay, TimeUnit timeUnit) {
        //注册队列
        this.registerQueue(queue, Constant.QueueType.ZSET);

        long epochSecond = getEpochSecond();

        //消息id
        String id = StringUtils.getUUID();
        redisTemplate.execute(new SessionCallback() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //保存消息支应的数据
                operations.opsForHash().put(Constant.getQueueDataKey(queue), id, JSONObject.toJSONString(message));

                operations.opsForZSet().add(
                        Constant.getQueueKey(queue),
                        id,
                        epochSecond + timeUnit.toSeconds(delay)
                );
                operations.exec();
                return null;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("发送延时队列消息：队列={}，消息={}，延迟时间={}", queue, JSONObject.toJSONString(message), timeUnit.toSeconds(delay) + "秒");
        }
    }

    /**
     * 向延迟队列批量发送消息
     * 所有消息的延时时长均相同
     *
     * @param queue       队列名
     * @param messageList 消息
     * @param delay       延迟时间
     * @param timeUnit    时间单位
     */
    public <T> void sendDelayMessageAll(String queue, List<T> messageList, long delay, TimeUnit timeUnit) {
        Assert.notEmpty(messageList, "消息不能为空");

        //注册队列
        this.registerQueue(queue, Constant.QueueType.ZSET);

        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toInstant(ZoneOffset.of("+8")).getEpochSecond();
        Double score = Double.valueOf(epochSecond + timeUnit.toSeconds(delay));

        //消息id
        String id = null;
        Object[] idArray = new Object[messageList.size()];
        Map paramMap = new HashMap();
        final Set<ZSetOperations.TypedTuple> typedTuples = new HashSet<>();
        int i = 0;
        for (T mess : messageList) {
            id = StringUtils.getUUID();
            paramMap.put(id, JSONObject.toJSONString(mess));
            idArray[i] = id;
            typedTuples.add(new DefaultTypedTuple(id, score));
            i++;
        }

        redisTemplate.execute(new SessionCallback() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //保存消息支应的数据
                operations.opsForHash().putAll(Constant.getQueueDataKey(queue), paramMap);

                operations.opsForZSet().add(Constant.getQueueKey(queue), typedTuples);
                operations.exec();
                return null;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("发送延时队列消息：队列={}，消息={}，延迟时间={}", queue, JSONObject.toJSONString(messageList), timeUnit.toSeconds(delay) + "秒");
        }
    }

    /**
     * 向延迟队列批量发送消息
     * 所有消息的延时时长各不相同
     *
     * @param queue       队列名
     * @param messageList 消息
     */
    public void sendDelayMessageAll(String queue, List<DelayMessageParam> messageList) {
        Assert.notEmpty(messageList, "消息不能为空");

        //注册队列
        this.registerQueue(queue, Constant.QueueType.ZSET);

        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toInstant(ZoneOffset.of("+8")).getEpochSecond();

        //消息id
        String id = null;
        Object[] idArray = new Object[messageList.size()];
        Map paramMap = new HashMap();
        final Set<ZSetOperations.TypedTuple> typedTuples = new HashSet<>();
        int i = 0;
        for (DelayMessageParam param : messageList) {
            id = StringUtils.getUUID();
            paramMap.put(id, JSONObject.toJSONString(param.getData()));
            idArray[i] = id;
            typedTuples.add(new DefaultTypedTuple(id, Double.valueOf(epochSecond + param.getTimeUnit().toSeconds(param.getDelay()))));
            i++;
        }

        redisTemplate.execute(new SessionCallback() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //保存消息支应的数据
                operations.opsForHash().putAll(Constant.getQueueDataKey(queue), paramMap);

                operations.opsForZSet().add(Constant.getQueueKey(queue), typedTuples);
                operations.exec();
                return null;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("发送延时队列消息：队列={}，消息={}", queue, JSONObject.toJSONString(messageList));
        }
    }


    /**
     * 向队列发送消息
     *
     * @param queue   队列名
     * @param message 消息
     */
    public <T> void sendMessage(String queue, T message) {
        //注册队列
        this.registerQueue(queue, Constant.QueueType.LIST);

        //消息id
        String id = StringUtils.getUUID();
        redisTemplate.execute(new SessionCallback() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //保存消息对应的数据
                operations.opsForHash().put(Constant.getQueueDataKey(queue), id, JSONObject.toJSONString(message));

                operations.opsForList().rightPush(Constant.getQueueKey(queue), id);
                operations.exec();
                return null;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("发送队列消息：队列={}，消息={}", queue, JSONObject.toJSONString(message));
        }
    }

    /**
     * 向队列批量发送消息
     *
     * @param queue       队列名
     * @param messageList 消息列表
     */
    public <T> void sendMessageAll(String queue, List<T> messageList) {
        Assert.notEmpty(messageList, "消息不能为空");

        //注册队列
        this.registerQueue(queue, Constant.QueueType.LIST);

        //消息id
        String id = null;
        Object[] idArray = new Object[messageList.size()];
        Map paramMap = new HashMap();
        int i = 0;
        for (T mess : messageList) {
            id = StringUtils.getUUID();
            paramMap.put(id, JSONObject.toJSONString(mess));
            idArray[i] = id;
            i++;
        }

        redisTemplate.execute(new SessionCallback() {
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //保存消息对应的数据
                operations.opsForHash().putAll(Constant.getQueueDataKey(queue), paramMap);

                operations.opsForList().rightPushAll(Constant.getQueueKey(queue), idArray);
                operations.exec();
                return null;
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("发送队列消息：队列={}，消息={}", queue, JSONObject.toJSONString(messageList));
        }
    }


    /**
     * 返回当前秒级时间戳
     *
     * @return
     */
    private long getEpochSecond() {
        LocalDateTime now = LocalDateTime.now();
        return now.toInstant(ZoneOffset.of("+8")).getEpochSecond();
    }

    /**
     * 从延迟队列中获取消息
     *
     * @param annotation
     * @return
     * @throws InterruptedException
     */
    public List<Message<?>> takeDelayMessage(Annotation annotation) {
        final String key = Constant.getQueueKey(annotation.getQueue());
        return this.takeMessage0(
                annotation,
                () -> redisTemplate.opsForZSet().rangeByScore(key, 0, getEpochSecond(), 0, Math.max(annotation.getPrefetch(), 1)),
                ((elements, messageMap) -> redisTemplate.execute(new SessionCallback<List<Message<?>>>() {
                    @Override
                    public List<Message<?>> execute(RedisOperations operations) throws DataAccessException {
                        operations.multi();

                        //删除元素
                        String[] params = new String[elements.size()];
                        operations.opsForZSet().remove(key, elements.toArray(params));

                        List<Message<?>> list = elements.stream().map(messageId -> {
                            Message<?> data = messageMap.get(messageId);
                            if (data == null) {
                                log.warn("消息数据为空：messageId={}", messageId);
                                return null;
                            }

                            data.setType(Constant.QueueType.ZSET);

                            if (!annotation.isAutoAck()) {
                                //手动确认时，先加消息添加到待确认
                                operations.opsForZSet().add(Constant.getAckQueueKey(), buildUnAckMessage(messageId, annotation.getQueue(), Constant.QueueType.ZSET), getEpochSecond());

                            } else {
                                //自动确认时，需要自动删除消息对应的数据
                                redisTemplate.opsForHash().delete(Constant.getQueueDataKey(annotation.getQueue()), messageId);
                            }
                            return data;
                        }).filter(e -> e != null).collect(Collectors.toList());

                        operations.exec();
                        return list;
                    }
                })
                )
        );
    }

    /**
     * 将待确认消息重新入队（2小时前待确认数据）
     *
     * @param annotation
     * @return
     * @throws InterruptedException
     */
    private void unAckMessageRequeue0(Annotation annotation) {
        final String key = Constant.getAckQueueKey();
        this.takeMessage0(
                annotation,
                () -> redisTemplate.opsForZSet().rangeByScore(key, 0, getEpochSecond() - 3600 * 2, 0, Math.max(annotation.getPrefetch(), 1)),
                ((elements, messageMap) -> {
                    redisTemplate.execute(new SessionCallback<Void>() {
                        @Override
                        public Void execute(RedisOperations operations) throws DataAccessException {
                            operations.multi();

                            //从待确认队列中删除
                            String[] params = new String[elements.size()];
                            operations.opsForZSet().remove(key, elements.toArray(params));

                            //将消息加到原来的队列中
                            elements.stream()
                                    .forEach(element -> {
                                        String[] array = element.split(";");
                                        String messageId = array[0];
                                        String queueKey = Constant.getQueueKey(array[1]);
                                        int type = Integer.valueOf(array[2]);
                                        if (Constant.QueueType.LIST == type) {
                                            //从左侧插入，使其优先被消费
                                            operations.opsForList().leftPush(queueKey, messageId);
                                        } else if (Constant.QueueType.ZSET == type) {
                                            operations.opsForZSet().add(queueKey, messageId, getEpochSecond());
                                        } else {
                                            log.warn("不支持的待确认消息类型：{}", type);
                                            return;
                                        }
                                    });

                            operations.exec();
                            return null;
                        }
                    });
                    return null;
                })
        );
    }

    /**
     * 待确认消息重新入队
     */
    public void unAckMessageRequeue(int consumers) {
        //将待确认的消息重新入队
        Annotation annotation = new Annotation();
        annotation.setConsumers(consumers);
        annotation.setQueue(Constant.getAckQueueKey());
        annotation.setPrefetch(200);
        annotation.setFrequency(60 * 15);//15分钟
//        annotation.setFrequency(15);//15秒

        //消费者个数
        for (int i = 1; i <= Math.max(1, annotation.getConsumers()); i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        this.unAckMessageRequeue0(annotation);
                    } catch (Exception e) {
                        log.error("待确认消息重新入队出错：{}", e.getMessage(), e);
                    }
                }
            }).start();
        }
    }

    /**
     * 从队列中获取消息基础方法
     *
     * @param annotation
     * @param elemSupplier 查询队列数据的函数
     * @param resultFun    获取返回值函数
     * @return
     * @throws InterruptedException
     */
    public List<Message<?>> takeMessage0(Annotation annotation, Supplier<Collection<String>> elemSupplier, BiFunction<Collection<String>, Map<String, Message>, List<Message<?>>> resultFun) {
        //查询频率
        if (annotation.getFrequency() < 1) {
            annotation.setFrequency(2);
        }

        //标志，用于在应启动时就能执行一次，而不用等待
        boolean flag = false;

        while (true) {
            if (flag) {
                try {
                    TimeUnit.SECONDS.sleep(annotation.getFrequency());
                } catch (InterruptedException e) {
                    //忽略
                }
            } else {
                flag = true;
            }

            //查询队列中的元素
            Collection<String> elements = elemSupplier.get();
            if (elements == null || elements.isEmpty()) {
                continue;
            }

            //加锁
            String lockKey = Constant.getLockKey(annotation.getQueue());
            String lockId = null;
            try {
                lockId = distributedLock.tryLock(lockKey, 3000, 180);
            } catch (RedisDistributedLockException e) {
                //未抢到锁，忽略
                continue;
            }

            try {
                //再次查询确认
                elements = elemSupplier.get();
                if (elements == null || elements.isEmpty()) {
                    continue;
                }

                //批量查询消息对应的数据
                Map<String, Message> messageMap = new HashMap<>();
                if (annotation.getClazz() != null) {
                    List<Object> hashKeyList = new ArrayList<>(elements);
                    List<Object> dataList = redisTemplate.opsForHash().multiGet(Constant.getQueueDataKey(annotation.getQueue()), hashKeyList);

                    for (int i = 0; i < hashKeyList.size(); i++) {
                        String id = (String) hashKeyList.get(i);
                        Message message = new Message();
                        message.setId(id);
                        message.setQueue(annotation.getQueue());
                        message.setData(JSONObject.parseObject((String) dataList.get(i), annotation.getClazz()));
                        if (message.getData() == null) {
                            log.warn("message data is null: id={}, queue={}", id, message.getQueue());
                            continue; //一般不会发生
                        }
                        messageMap.put(id, message);
                    }
                }

                return resultFun.apply(elements, messageMap);
            } finally {
                distributedLock.unlock(lockKey, lockId);
            }
        }
    }


    /**
     * 从队列中获取消息
     *
     * @param annotation
     * @return
     * @throws InterruptedException
     */
    public List<Message<?>> takeMessage(Annotation annotation) {
        final String key = Constant.getQueueKey(annotation.getQueue());
        return this.takeMessage0(
                annotation,
                () -> redisTemplate.opsForList().range(key, 0, annotation.getPrefetch()),
                ((elements, messageMap) -> redisTemplate.execute(new SessionCallback<List<Message<?>>>() {
                    @Override
                    public List<Message<?>> execute(RedisOperations operations) throws DataAccessException {
                        operations.multi();

                        List<Message<?>> list = elements.stream().map(messageId -> {
                            //删除消息
                            operations.opsForList().remove(key, 1, messageId);

                            Message<?> data = messageMap.get(messageId);
                            if (data == null) {
                                log.warn("消息数据为空：messageId={}", messageId);
                                return null;
                            }

                            data.setType(Constant.QueueType.LIST);

                            if (!annotation.isAutoAck()) {
                                //手动确认时，先加消息添加到待确认
                                //待确认消息的格式：消息id;队列
                                operations.opsForZSet().add(
                                        Constant.getAckQueueKey(),
                                        buildUnAckMessage(messageId, annotation.getQueue(), Constant.QueueType.LIST),
                                        getEpochSecond());

                            } else {
                                //自动确认时，需要自动删除消息对应的数据
                                operations.opsForHash().delete(Constant.getQueueDataKey(annotation.getQueue()), messageId);
                            }
                            return data;
                        }).filter(e -> e != null).collect(Collectors.toList());

                        operations.exec();
                        return list;
                    }
                }))
        );
    }

    private String buildUnAckMessage(String messageId, String queue, int type) {
        return messageId + ";" + queue + ";" + type;
    }


    /**
     * 确认消息
     *
     * @param message
     */
    public void ack(Message message) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //从待确认队列中删除
                operations.opsForZSet().remove(Constant.getAckQueueKey(), buildUnAckMessage(message.getId(), message.getQueue(), message.getType()));
                //删除消息对应的数据
                operations.opsForHash().delete(Constant.getQueueDataKey(message.getQueue()), message.getId());
                operations.exec();
                return null;
            }
        });
    }


    /**
     * 消息重新入队，并放到队首，优先消费
     *
     * @param message
     */
    public void requeue(Message message) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //从待确认队列中删除
                operations.opsForZSet().remove(Constant.getAckQueueKey(), buildUnAckMessage(message.getId(), message.getQueue(), message.getType()));
                //重新入队
                if (message.getType() == Constant.QueueType.LIST) {
                    operations.opsForList().leftPush(Constant.getQueueKey(message.getQueue()), message.getId());
                }else {
                    operations.opsForZSet().add(
                            Constant.getQueueKey(message.getQueue()),
                            message.getId(),
                            getEpochSecond()
                    );
                }
                operations.exec();
                return null;
            }
        });
    }


    /**
     * 获取队列长度
     *
     * @param queue
     * @return
     */
    public long queueSize(String queue) {
        String key = Constant.getQueueKey(queue);
        DataType type = redisTemplate.type(key);
        if (type == null) {
            return 0;
        }

        if (type == DataType.LIST) {
            return redisTemplate.opsForList().size(key);
        } else if (type == DataType.ZSET) {
            return redisTemplate.opsForZSet().size(key);
        } else if (type == DataType.SET) {
            return redisTemplate.opsForSet().size(key);
        } else if (type == DataType.HASH) {
            return redisTemplate.opsForHash().size(key);
        }

        return 0;
    }

    /**
     * 注册队列
     *
     * @param queue
     */
    public void registerQueue(String queue, int type) {
        //判断类型是否匹配
        Integer oldType = allQueueMap.get(queue + ";" + Constant.getVirtualHost());
        if (oldType != null) {
            if (oldType != type) {
                throw new RuntimeException("队列类型不匹配");
            }
            return;
        }

        String key = Constant.getAllQueueKey();
        String element = queue + ";" + Constant.getVirtualHost() + ";" + type;
        redisTemplate.opsForSet().add(key, element);

        if (oldType == null) {
            //发布消息
            this.sendChannelMessage(Constant.REFRESH_ALL_QUEUE_CACHE_CHANNEL);
        }
    }


    /**
     * 发布与订阅模式下发送消息
     *
     * @param channel 要订阅的频道
     * @param <T>
     */
    public <T> void sendChannelMessage(String channel) {
        redisTemplate.convertAndSend(channel, "");
    }

    /**
     * 发布与订阅模式下发送消息
     *
     * @param channel 要订阅的频道
     * @param message 要发送的消息，不能为null
     * @param <T>
     */
    public <T> void sendChannelMessage(String channel, T message) {
        redisTemplate.convertAndSend(channel, JSONObject.toJSONString(message));
    }
}

