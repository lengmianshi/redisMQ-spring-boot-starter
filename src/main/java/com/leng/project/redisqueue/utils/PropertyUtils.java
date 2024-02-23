package com.leng.project.redisqueue.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

public class PropertyUtils {
    private static Environment env;
    private static ApplicationContext applicationContext;

    @Autowired
    public void setEnv(Environment env) {
        PropertyUtils.env = env;
    }

    @Autowired
    public void setApplicationContext(ApplicationContext context) {
        PropertyUtils.applicationContext = context;
    }

    /**
     * 确保bean被spring容器实例化后才使用调用
     *
     * @param key
     * @return
     */
    public static String getProperty(String key) {
        return env.getProperty(key);
    }

    /**
     * 确保bean被spring容器实例化后才使用调用
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getProperty(String key, Class<T> clazz) {
        return env.getProperty(key, clazz);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

}
