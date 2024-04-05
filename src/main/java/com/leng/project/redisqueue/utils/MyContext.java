package com.leng.project.redisqueue.utils;


import java.util.HashMap;
import java.util.Map;

/**
 * 全局上下文工具类,用于储存一些东西
 */
public class MyContext {
    private static final ThreadLocal<Map<Object, Object>> myContext = new ThreadLocal<Map<Object, Object>>() {
        @Override
        protected Map<Object, Object> initialValue() {
            return new HashMap<>();
        }

    };

    /**
     * 根据key获取值
     * @param key
     * @return
     */
    public static Object getValue(Object key) {
        if(myContext.get() == null) {
            return null;
        }
        return myContext.get().get(key);
    }

    /**
     * 存储
     * @param key
     * @param value
     * @return
     */
    public static Object setValue(Object key, Object value) {
        Map<Object, Object> cacheMap = myContext.get();
        if(cacheMap == null) {
            cacheMap = new HashMap<>();
            myContext.set(cacheMap);
        }
        return cacheMap.put(key, value);
    }

    /**
     * 根据key移除值
     * @param key
     */
    public static void removeValue(Object key) {
        Map<Object, Object> cacheMap = myContext.get();
        if(cacheMap != null) {
            cacheMap.remove(key);
        }
    }

    /**
     * 重置
     */
    public static void reset() {
        if(myContext.get() != null) {
            myContext.get().clear();
        }
    }

}
