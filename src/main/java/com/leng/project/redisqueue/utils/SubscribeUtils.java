package com.leng.project.redisqueue.utils;

import com.leng.project.redisqueue.Constant;

public class SubscribeUtils {

    /**
     * 获取当前channel名称，如果topic使用了通配符，可能需要知道确切的channel
     * @return
     */
    public static String currentChannel(){
        return (String) MyContext.getValue(Constant.CURRENT_SUBSCRIBE_CHANNEL_KEY);
    }
}
