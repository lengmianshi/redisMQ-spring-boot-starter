package com.leng.project.redisqueue.exception;

public class AckException extends RuntimeException {

    public AckException(Throwable t){
        super(t);
    }
}
