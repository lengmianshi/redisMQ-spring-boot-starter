package com.leng.project.redisqueue.filter;


import com.alibaba.fastjson.JSON;
import com.leng.project.redisqueue.Constant;
import com.leng.project.redisqueue.exception.AppException;
import com.leng.project.redisqueue.properties.QueueProperty;
import com.leng.project.redisqueue.response.AppResult;
import com.leng.project.redisqueue.utils.PropertyUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 给token续命
 */
//@WebFilter(urlPatterns = {"/queue/*"}, filterName = "queueTokenFilter")
@Slf4j
public class TokenFilter implements Filter {
//    @Autowired
    private StringRedisTemplate redisTemplate;
//    @Autowired
    private QueueProperty queueProperty;

    private StringRedisTemplate redisTemplate(){
        if(this.redisTemplate == null){
            redisTemplate = PropertyUtils.getBean(StringRedisTemplate.class);
        }
        return redisTemplate;
    }

    private QueueProperty queueProperty(){
        if(this.queueProperty == null){
            queueProperty = PropertyUtils.getBean(QueueProperty.class);
        }
        return queueProperty;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = ((HttpServletRequest) request);

        try {
            String token = httpRequest.getHeader("Queue-Token");
            if (token != null && !token.isEmpty()) {
                String key = Constant.getTokenKey(token);
                if (!redisTemplate().hasKey(key)) {
                    throw new AppException(403, "会话已过期，请重新登录");
                }

                //续命
                redisTemplate().expire(key, queueProperty().getConsole().getSessionExpiration(), TimeUnit.SECONDS);
            }

            chain.doFilter(httpRequest, response);
        } catch (AppException e) {
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().write(JSON.toJSONString(AppResult.build(e.getStatus(), e.getMessage())));
        } catch (Exception e) {
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/json");
            response.getWriter().write(JSON.toJSONString(AppResult.build(500, e.getMessage())));
            log.error("", e);
        }
    }

    @Override
    public void destroy() {

    }

}
