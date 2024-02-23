package com.leng.project.redisqueue.controller;

import com.leng.project.redisqueue.Constant;
import com.leng.project.redisqueue.bean.Queue;
import com.leng.project.redisqueue.properties.QueueProperty;
import com.leng.project.redisqueue.response.AppResult;
import com.leng.project.redisqueue.service.QueueService;
import com.leng.project.redisqueue.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("queue")
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "queue.console", name = "enable", havingValue = "true", matchIfMissing = true)
public class QueueController {
    @Autowired
    private QueueService queueService;
    @Autowired
    private QueueProperty queueProperty;
    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 队列列表
     *
     * @return
     */
    @GetMapping("list")
    @ResponseBody
    public Object list(@RequestParam("vhost")String vhost) {
        List<Queue> list = queueService.list(vhost);
        return AppResult.ok(list);
    }


    /**
     * 虚拟空间列表
     *
     * @return
     */
    @GetMapping("vhostList")
    @ResponseBody
    public Object vhostList() {
        List<String> list = queueService.vhostList();
        return AppResult.ok(list);
    }

    /**
     * 清空队列
     *
     * @param vhost
     * @param queue
     * @return
     */
    @PostMapping("clear")
    @ResponseBody
    public Object clear(@RequestParam("vhost") String vhost, @RequestParam("queue") String queue) {
        queueService.clear(vhost, queue);
        return AppResult.ok();
    }

    /**
     * 删除队列
     *
     * @param vhost
     * @param queue
     * @return
     */
    @PostMapping("del")
    @ResponseBody
    public Object del(@RequestParam("vhost") String vhost, @RequestParam("queue") String queue, @RequestParam("queueType") int queueType) {
        queueService.del(vhost, queue, queueType);
        return AppResult.ok();
    }

    /**
     * 登录
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("login")
    @ResponseBody
    public Object login(@RequestParam("username") String username, @RequestParam("password") String password) {
        if (!Objects.equals(queueProperty.getConsole().getUsername(), username) || !Objects.equals(queueProperty.getConsole().getPassword(), password)) {
            return AppResult.build(400, "用户名或密码不正确");
        }

        String token = StringUtils.getUUID();
        redisTemplate.opsForValue().set(Constant.getTokenKey(token), "1", queueProperty.getConsole().getSessionExpiration(), TimeUnit.SECONDS);
        return AppResult.ok(token);
    }

    /**
     * 登出
     *
     * @return
     */
    @PostMapping("logout")
    @ResponseBody
    public Object logout(@RequestHeader(value = "Queue-Token", required = false) String token) {
        if (token != null) {
            redisTemplate.delete(Constant.getTokenKey(token));
        }
        return AppResult.ok();
    }
}
