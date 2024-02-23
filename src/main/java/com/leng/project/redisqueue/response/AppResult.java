package com.leng.project.redisqueue.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;

@Data
public class AppResult<T> {

    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 响应业务状态
    private Integer status;

    // 响应消息
    private String msg;

    // 响应中的数据
    private T data;

    public static <T> AppResult build(Integer status, String msg, T data) {
        return new AppResult(status, msg, data);
    }

    public static <T> AppResult ok(T data) {
        return new AppResult(data);
    }

    public static AppResult ok() {
        return new AppResult(null);
    }


    public static AppResult build(Integer status, String msg) {
        return new AppResult(status, msg, null);
    }

    public AppResult(Integer status, String msg, T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public AppResult(T data) {
        this.status =200;
        this.msg = "OK";
        this.data = data;
    }


    /**
     * 没有object对象的转化
     *
     * @param json json字符串
     * @return AppResult
     */
    public static AppResult format(String json) throws IOException {
        return MAPPER.readValue(json, AppResult.class);
    }


}
