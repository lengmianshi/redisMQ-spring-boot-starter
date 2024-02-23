package com.leng.project.redisqueue.service;

import com.leng.project.redisqueue.Constant;
import com.leng.project.redisqueue.bean.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class QueueService {
    @Autowired
    private StringRedisTemplate redisTemplate;


    public List<Queue> list(String vhost) {
        Set<String> members = redisTemplate.opsForSet().members(Constant.getAllQueueKey());
        if (members == null) {
            return new ArrayList<>();
        }

        List<Queue> list = members.stream()
                .map(e -> {
                    String[] array = e.split(";");
                    if (vhost != null && vhost.length() > 0) {
                        if (!Objects.equals(vhost, array[1])) {
                            return null;
                        }
                    }

                    Queue queue = new Queue();
                    queue.setQueue(array[0]);
                    queue.setVirtualHost(array[1]);
                    queue.setQueueType(Integer.valueOf(array[2]));

                    //查询总消息数
                    Long size = null;
                    if (queue.getQueueType() == Constant.QueueType.LIST) {
                        size = redisTemplate.opsForList().size(Constant.getQueueKey(queue.getQueue(), queue.getVirtualHost()));
                    } else if (queue.getQueueType() == Constant.QueueType.ZSET) {
                        size = redisTemplate.opsForZSet().size(Constant.getQueueKey(queue.getQueue(), queue.getVirtualHost()));
                    }
                    queue.setTotal(size == null ? 0 : size);

                    return queue;
                }).filter(e -> e != null).sorted(Comparator.comparing(Queue::getVirtualHost)).collect(Collectors.toList());

        //查询所有虚拟空间的待确认数量
        Map<String, Map<String, Long>> unAckMap = new HashMap<>();
        list.stream()
                .map(e -> e.getVirtualHost())
                .distinct()
                .forEach(virtualHost -> {
                    Set<String> set = redisTemplate.opsForZSet().range(Constant.getAckQueueKey(virtualHost), 0, -1);
                    if (set == null) {
                        unAckMap.put(virtualHost, new HashMap<>());
                    } else {
                        Map<String, Long> map = set.stream()
                                .map(e -> e.split(";")[1])
                                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
                        unAckMap.put(virtualHost, map);
                    }
                });

        if (!unAckMap.isEmpty()) {
            list.stream()
                    .forEach(queue -> {
                        Map<String, Long> map = unAckMap.get(queue.getVirtualHost());
                        if (map != null) {
                            queue.setUnAcked(map.getOrDefault(queue.getQueue(), 0L));
                        }
                    });
        }
        return list;
    }

    public void clear(String vhost, String queue) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                //删除队列
                String key = Constant.getQueueKey(queue, vhost);
                operations.delete(key);

                //删除消息对应的数据
                key = Constant.getQueueDataKey(queue, vhost);
                operations.delete(key);

                //删除待确认消息
                key = Constant.getAckQueueKey(vhost);
                Set<String> elements = operations.opsForZSet().range(key, 0, -1);
                if (elements != null && elements.size() > 0) {
                    List<String> removeList = new ArrayList<>();
                    elements.forEach(e -> {
                        if (e.split(";")[1].equals(queue)) {
                            removeList.add(e);
                        }
                    });

                    if (removeList.size() > 0) {
                        operations.opsForZSet().remove(key, removeList);
                    }
                }

                operations.exec();
                return null;
            }
        });
    }

    public void del(String vhost, String queue, int queueType) {
        this.clear(vhost, queue);
        redisTemplate.opsForSet().remove(Constant.getAllQueueKey(), queue + ";" + vhost + ";" + queueType);
    }

    public List<String> vhostList() {
        Set<String> members = redisTemplate.opsForSet().members(Constant.getAllQueueKey());
        if (members == null) {
            return new ArrayList<>();
        }

        return members.stream().map(e -> e.split(";")[1]).distinct().collect(Collectors.toList());
    }
}
