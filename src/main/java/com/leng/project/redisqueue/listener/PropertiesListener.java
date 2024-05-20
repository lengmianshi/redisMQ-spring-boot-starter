package com.leng.project.redisqueue.listener;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class PropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Properties props = new Properties();
        //开启gzip压缩
        props.put("server.compression.enabled", "true");
        props.put("server.compression.mime-types", "text/css,text/javascript,application/javascript");
        environment.getPropertySources().addFirst(new PropertiesPropertySource("redisQueue", props));
    }
}