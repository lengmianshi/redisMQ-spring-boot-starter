package com.leng.project.redisqueue.utils;

import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class RetryUtils {

    /**
     * 获取RetryTemplate实例
     *
     * @param maxAttempts   最多执行次数，最小为1
     * @param backOffPeriod 失败重试时的间隔，毫秒
     * @return
     */
    public static RetryTemplate getTemplate(int maxAttempts, long backOffPeriod) {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy oRetryPolicy = new SimpleRetryPolicy();
        oRetryPolicy.setMaxAttempts(maxAttempts);
        FixedBackOffPolicy offPolicy = new FixedBackOffPolicy();
        offPolicy.setBackOffPeriod(backOffPeriod);
        template.setRetryPolicy(oRetryPolicy);
        template.setBackOffPolicy(offPolicy);
        return template;
    }
}
