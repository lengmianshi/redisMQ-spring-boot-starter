package com.leng.project.redisqueue.exception;



/**
 * 自定义异常
 */
public class AppException extends RuntimeException {
    private int status = 400;//默认状态

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable t) {
        super(message, t);
    }

    public AppException(int status, String message) {
        super(message);
        this.status = status;

    }

    public AppException(int status, String message, Throwable t) {
        super(message, t);
        this.status = status;

    }

    public int getStatus() {
        return status;
    }
}
