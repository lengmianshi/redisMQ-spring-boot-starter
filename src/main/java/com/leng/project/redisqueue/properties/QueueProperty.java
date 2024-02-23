package com.leng.project.redisqueue.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "queue")
//@ConditionalOnWebApplication//web应用才生效
public class QueueProperty {
    //控制台
    private Console console = new Console();
    //虚拟空间
    private String virtualHost = "/";


    @Data
    public static class Console {
        //是否启动控制台
        private boolean enable = true;
        //用户名
        private String username = "admin";
        //密码
        private String password = "admin";
        //会话到期时长，单位秒
        private long sessionExpiration = 1800;
    }
}

