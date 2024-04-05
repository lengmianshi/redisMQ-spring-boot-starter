package com.leng.project.redisqueue.config;

import com.leng.project.redisqueue.RedisQueueRunner;
import com.leng.project.redisqueue.RedisQueueTemplate;
import com.leng.project.redisqueue.filter.TokenFilter;
import com.leng.project.redisqueue.handler.QueueHandler;
import com.leng.project.redisqueue.handler.SubscribeHandler;
import com.leng.project.redisqueue.lock.RedisDistributedLock;
import com.leng.project.redisqueue.properties.QueueProperty;
import com.leng.project.redisqueue.service.QueueService;
import com.leng.project.redisqueue.utils.PropertyUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@EnableConfigurationProperties(QueueProperty.class)
@ComponentScan("com.leng.project.redisqueue.controller")
public class RedisQueueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisQueueTemplate.class)
    public RedisQueueTemplate redisQueueTemplate() {
        return new RedisQueueTemplate();
    }

    @Bean
    @ConditionalOnMissingBean(RedisDistributedLock.class)
    public RedisDistributedLock redisDistributedLock() {
        return new RedisDistributedLock();
    }

    @Bean
    @ConditionalOnMissingBean(QueueHandler.class)
    public QueueHandler queueHandler() {
        return new QueueHandler();
    }

    @Bean
    @ConditionalOnMissingBean(SubscribeHandler.class)
    public SubscribeHandler subscribeHandler() {
        return new SubscribeHandler();
    }

    @Bean
    @ConditionalOnMissingBean(RedisQueueRunner.class)
    public RedisQueueRunner redisQueueRunner() {
        return new RedisQueueRunner();
    }

    @Bean
    @ConditionalOnMissingBean(PropertyUtils.class)
    public PropertyUtils propertyUtils() {
        return new PropertyUtils();
    }


    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }

    @Bean
    @ConditionalOnProperty(prefix = "queue.console", name = "enable", havingValue = "true", matchIfMissing = true)
    public QueueService queueService() {
        return new QueueService();
    }

    //    @Bean(name = "druidStatView")
//    public ServletRegistrationBean druidStatView() {
//        ServletRegistrationBean registrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
//        //IP白名单 (没有配置或者为空，则允许所有访问)
//        registrationBean.addInitParameter("allow", "");
//        //IP黑名单 (存在共同时，deny优先于allow)
//        registrationBean.addInitParameter("deny", "");
//        registrationBean.addInitParameter("loginUsername", "admin");
//        registrationBean.addInitParameter("loginPassword", "Mjk90df@f@#d");
//        registrationBean.addInitParameter("resetEnable", "false");
//        return registrationBean;
//    }
//
    @Bean(name = "queueTokenFilter")
    public FilterRegistrationBean queueTokenFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(new TokenFilter());
        registrationBean.setName("queueTokenFilter");
        registrationBean.addUrlPatterns("/queue/*");
        registrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico, *.html");
        return registrationBean;
    }
}
