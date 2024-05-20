package com.leng.project.redisqueue.exception;

public class ConsumeException extends RuntimeException{

    public ConsumeException(Throwable t){
        super(t);
    }
}
