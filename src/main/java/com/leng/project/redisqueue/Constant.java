package com.leng.project.redisqueue;

import com.leng.project.redisqueue.properties.QueueProperty;
import com.leng.project.redisqueue.utils.PropertyUtils;

public class Constant {
    /**
     * 虚拟空间
     */
    private static String virtualHost = null;

    /**
     * 所有相关key的前缀
     */
    private static final String PREFIX = "QUEUE:";

    /**
     * 当前channel的key，要线程本地化变量中使用
     */
    public static final String CURRENT_SUBSCRIBE_CHANNEL_KEY = "current_subscribe_channel";

    /**
     * 刷新队列缓存的主题
     */
    public static final String REFRESH_ALL_QUEUE_CACHE_CHANNEL = "refresh_all_queue_cache";


    /**
     * 队列的数据结构
     */
    public interface QueueType {
        int LIST = 1;
        int ZSET = 2;
    }


    /**
     * 获取虚拟空间
     *
     * @return
     */
    public static String getVirtualHost() {
        if (virtualHost == null) {
            QueueProperty property = PropertyUtils.getBean(QueueProperty.class);
            String vhost = property.getVirtualHost();
            if (vhost == null || vhost.trim().isEmpty()) {
                vhost = "/";
            }

            if (!vhost.startsWith("/")) {
                vhost += "/";
            }

            virtualHost = vhost;
        }

        return virtualHost;
    }


    /**
     * 获取队列的key
     *
     * @param queue
     * @return
     */
    public static String getQueueKey(String queue) {
        return Constant.PREFIX + getVirtualHost() + ":" + queue;
    }

    /**
     * 获取队列的key
     *
     * @param queue
     * @return
     */
    public static String getQueueKey(String queue, String virtualHost) {
        return Constant.PREFIX + virtualHost + ":" + queue;
    }


    /**
     * 获取待确认消息集合的key，zset
     *
     * @return
     */
    public static String getAckQueueKey() {
        return PREFIX + getVirtualHost() + ":ack";
    }

    /**
     * 获取待确认消息集合的key，zset
     *
     * @return
     */
    public static String getAckQueueKey(String virtualHost) {
        return PREFIX + virtualHost + ":ack";
    }

    public static String getLockKey(String queue) {
        return Constant.PREFIX + getVirtualHost() + ":lk:" + queue;
    }


    /**
     * 消息对应的数据，hash结构
     *
     * @param queue
     * @return
     */
    public static String getQueueDataKey(String queue) {
        return Constant.PREFIX + getVirtualHost() + ":data:" + queue;
    }

    /**
     * 消息对应的数据，hash结构
     *
     * @param queue
     * @return
     */
    public static String getQueueDataKey(String queue, String virtualHost) {
        return Constant.PREFIX + virtualHost + ":data:" + queue;
    }

    /**
     * 保存所有队列的key，set结构
     *
     * @return
     */
    public static String getAllQueueKey() {
        return Constant.PREFIX + "queues";
    }

    /**
     * token对应的key
     *
     * @param token
     * @return
     */
    public static String getTokenKey(String token) {
        return Constant.PREFIX + "token:" + token;
    }
}
