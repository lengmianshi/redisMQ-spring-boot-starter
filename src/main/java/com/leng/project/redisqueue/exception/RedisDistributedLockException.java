package com.leng.project.redisqueue.exception;


/**
 * 自定义异常
 */
public class RedisDistributedLockException extends RuntimeException {

    public RedisDistributedLockException(String message) {
        super(message);
    }


    public RedisDistributedLockException(String message, Throwable t) {
        super(message, t);

    }

}
