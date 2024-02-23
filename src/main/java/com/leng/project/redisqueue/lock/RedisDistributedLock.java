package com.leng.project.redisqueue.lock;

import com.leng.project.redisqueue.exception.RedisDistributedLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisDistributedLock {
    //    private static final String LOCK_SUCCESS = "OK";
//    private static final String SET_IF_NOT_EXIST = "NX";
//    private static final String EXPIRE_TIME_UNITS = "EX";//EX|PX, expire time units: EX = seconds; PX = milliseconds
//    private static final Long RELEASE_SUCCESS = 1L;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 加锁
     * 如果存在事务，在事务结束后会自动释放锁；如果不存在事务，则可以手动释放锁，也可以让key自动过期
     *
     * @param lockKey        锁的key
     * @param acquireTimeout 获取超时时间（单位毫秒）
     * @param expireTime     锁的超时时间（单位秒）
     * @return 锁标识
     */
    public String tryLock(final String lockKey, long acquireTimeout, int expireTime) throws RedisDistributedLockException{
        // 随机生成一个value
        final String identifier = UUID.randomUUID().toString().replaceAll("-", "");
        // 锁名，即key值
        // 获取锁的超时时间，超过这个时间则放弃获取锁
        long end = System.currentTimeMillis() + acquireTimeout;

        boolean res = false;

        while (System.currentTimeMillis() < end) {
            boolean flag = redisTemplate.boundValueOps(lockKey).setIfAbsent(identifier, expireTime, TimeUnit.SECONDS);
            if (flag) {
                res = true;
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!res) {
            throw new RedisDistributedLockException("系统繁忙，请稍后再试");
        }

        //如果在事务中，则在事务结束后自动释放锁
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    //释放锁
                    unlock(lockKey, identifier);
                }
            });
        }

        return identifier;
    }

    /**
     * 释放锁
     *
     * @param lockKey
     * @param identifier
     * @return
     */
    public void unlock(String lockKey, String identifier) {
//        String lockKey = "lock:" + lockKey;
//        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//        Object result = jedisClient.eval(script, Collections.singletonList(lockKey), Collections.singletonList(identifier));
//
//        return RELEASE_SUCCESS.equals(result);

        if (identifier.equals(redisTemplate.boundValueOps(lockKey).get())) {
            redisTemplate.delete(lockKey);
        }
    }
}
