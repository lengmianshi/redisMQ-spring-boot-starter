# 简介

消息队列（Message Queue）是一种使用队列（Queue）作为底层存储数据结构，可以用于解决不同进程与应用程序之间通讯的分布式消息容器，也可以称为消息中间件。

目前比较常用的消息队列有ActiveMQ、RabbitMQ、Kafka、RocketMQ、Redis等。


为什么要开发本项目？

因为Redis应用实在是非常广泛，基本上每个项目都离不开它，而常用的消息队列中间件，如RabbitMQ，则有些"笨重"；对于中小型项目，显得"大材小用"了，并且需要额外的运维成本；
虽然Redis支持消息队列，但直接使用并不方便，要作很多细节处理，而且代码移植也是个问题……

    

本项目的优点？
- 开箱即用，你几乎不用添加额外的配置
- 支持消息队列、延时队列，并提供精细化配置参数
- 提供消息确认机制
- 支持发布与订阅模式
- 支持虚拟空间，不同虚拟空间的数据互相隔离
- 支持web控制台，实时查看各个队列的消费情况



# 开始使用

## 引用依赖
springboot3.0以下版本:
```xml
<dependency>
    <groupId>io.github.lengmianshi</groupId>
    <artifactId>redisMQ-spring-boot-starter</artifactId>
    <version>1.0.5</version>
</dependency>

<!-- 以下配置可以改为你自己的版本 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>2.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
```
注：`spring-boot-starter-data-redis`依赖于`spring-data-redis`，如果发生依赖冲突，要确保`spring-data-redis`的版本不低于`2.1.0.RELEASE`，可在你的pom.xml中锁定版本：
```xml
<dependencyManagement>
    <dependencies>
          <dependency>
              <groupId>org.springframework.data</groupId>
              <artifactId>spring-data-redis</artifactId>
              <version>2.1.2.RELEASE</version>
          </dependency>
    </dependencies>
</dependencyManagement>
```

springboot3.0：
```xml
<dependency>
    <groupId>io.github.lengmianshi</groupId>
    <artifactId>redisMQ-spring-boot-starter</artifactId>
    <version>2.0.5</version>
</dependency>
        
<!-- 以下配置可以改为你自己的版本 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>3.2.1</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
```

## 配置redis
一般引入redis的项目都会事先配置，如果你的项目没配置过，则可在`application.yml`中加上如下配置：

springboot3.0以下版本：
```yaml
spring:
  redis:
    host: test.redis.com  #改成你的
    password: vC403V2KMc0Kghz #改成你的
    port: 6379 #改成你的
    jedis:
      pool:
        max-active: 100
        max-idle: 10
        min-idle: 10
    timeout: 2000
  # 覆盖重复注册的bean
  main:
    allow-bean-definition-overriding: true
```
springboot3.0：
```yaml
spring:
  data:
    redis:
      host: test.redis.com #改成你的
      password: vC403V2KMc0Kghz #改成你的
      port: 6379 #改成你的
      jedis:
        pool:
          max-active: 100
          max-idle: 10
          min-idle: 10
      timeout: 2000
  # 覆盖重复注册的bean
  main:
    allow-bean-definition-overriding: true
```




## 消息队列
### 生产者发送消息
```java
@Autowired
private RedisQueueTemplate redisQueueTemplate;

/**
 * 1次只发送一条消息
 */
@Test
public void test1() {
    JSONObject message = new JSONObject();
    message.put("bondId", "17f62f1dfb5afb12e8d67cd651c1df53");
    message.put("year", 2022);
    redisQueueTemplate.sendMessage("test_queue", message);
}

/**
 * 批量发送消息
 */
@Test
public void test2() {
    List messageList = new ArrayList<>();
    for (int i = 0; i < 5000; i++) {
        JSONObject mess = new JSONObject();
        mess.put("index", i);
        messageList.add(mess);
    }

    redisQueueTemplate.sendMessageAll("test_queue", messageList);
}
```
注：示例中每条MQ消息都用`JSONObject`包装，这只是我的个人习惯，你也可以使用实体类

### 消费者消费消息
消费方法的参数只能有1个，并且类型要与生产者发送消费的类型保存一致：
```java
@Component
public class QueueConsumer {
    //使用默认参数
    @RedisQueueListener(queue = "test_queue")
    public void test(JSONObject message){
        System.out.println(message);
    }

    //指定单个实例下使用5个消费线程
    @RedisQueueListener(queue = "test_queue2", consumers = 5)
    public void test2(JSONObject message){
        System.out.println(message);
    }

    //单个实例5个线程，手动确认
    @RedisQueueListener(queue = "test_queue3", consumers = 5, autoAck = false)
    public void test3(JSONObject message){
        System.out.println(message);
    }

}
```

`@RedisQueueListener`注解支持的所有参数：
```java
package com.leng.project.redisqueue.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisQueueListener {
    /**
     * 队列名
     *
     * @return
     */
    String queue() default "";

    /**
     * 消费者线程数
     *
     * @return
     */
    int consumers() default 1;

    /**
     * 是否自动确认
     *
     * @return
     */
    boolean autoAck() default true;

    /**
     * 一次从队列中取多少数据
     *
     * @return
     */
    int prefetch() default 50;

    /**
     * 获取消息的频率，单位秒
     * @return
     */
    long frequency() default 2;
}
```
其中：
- consumers：单个实例下启动多少个消费线程，默认为1
- autoAck：是否自动确认消息，默认为true。自动确认与手动确认的区别：
  - 自动确认：消费线程从队列中取出消息，如果消费失败，则该条消息丟失
  - 手动确认：消费线程从队列中取出消息，并将消息写入待确认队列中；如果消费失败，则一段时间后（15分钟）会重新入队，消费端要做幂等性处理
- prefetch：一个消费线程一次性从队列中取出多少条消息，因为涉及锁的竞争，不宜过小，默认为50
- frequency：单个消费线程每隔多少秒获取一次消息，默认为2，最小值为1。有人可能会奇怪，消息不是应该即时消费吗？不是越快越好吗？实际上，有些业务场景对消息的实时性要求很低，几天、几个月、甚至一年才执行一次，这时我们完全可以把frequency调大，以减轻redis的压力

## 延时队列
延时队列的常用场景如用户下单，xx分钟后没有支付则自动关闭订单；已支持的订单，xxx天后自动确认收货等。

### 生产者发送消息
```java
@Autowired
private RedisQueueTemplate redisQueueTemplate;

/**
 * 1次只发送1条消息
 */
public void test1(){
    JSONObject message = new JSONObject();
    message.put("bondId", "17f62f1dfb5afb12e8d67cd651c1df53");
    message.put("year", 2022);
    //延时5秒
    redisQueueTemplate.sendDelayMessage("test_delay_queue", message, 5, TimeUnit.SECONDS);
}

/**
 * 批量发送，每条消息的延时时长一样
 */
public void test2(){
    List messageList = new ArrayList<>();
    for (int i = 0; i < 5000; i++) {
        JSONObject mess = new JSONObject();
        mess.put("index", i);
        messageList.add(mess);
    }
    //延时5秒
    redisQueueTemplate.sendDelayMessageAll(queue, messageList, 5, TimeUnit.SECONDS);
}

/**
 * 批量发送，每条消息的延时时长各不相同
 */
public void test3(){
    List messageList=new ArrayList<>();
    for(int i=0; i< 5000; i++){
        JSONObject mess=new JSONObject();
        mess.put("index",i);
        
        //每条消息可以使用不同的延时时长，这里为了简便，统一写成5了
        DelayMessageParam param=new DelayMessageParam(mess,5,TimeUnit.SECONDS);
        messageList.add(param);
    }

    redisQueueTemplate.sendDelayMessageAll(queue,messageList);
}
```
注：示例中每条MQ消息都用`JSONObject`包装，这只是我的个人习惯，你也可以使用实体类

### 消费者消费消息
`@RedisDelayQueueListener`注解的参数与`@RedisQueueListener`完全相同；消费方法的参数只能有1个，并且类型要与生产者发送消费的类型保存一致：
```java
@Component
public class DelayQueueConsumer {
  /**
   * 使用默认参数
   * @param message
   */
  @RedisDelayQueueListener(queue = "test_delay_queue")
    public void test(JSONObject message){
        System.out.println(message);
    }

  /**
   * 单个实例5个消费线程
   * @param message
   */
  @RedisDelayQueueListener(queue = "test_delay_queue2", consumers = 5)
    public void test2(JSONObject message){
        System.out.println(message);
    }

  /**
   * 单个实例5个消费线程，手动确认
   * @param message
   */
  @RedisDelayQueueListener(queue = "test_delay_queue3", consumers = 5, autoAck = false)
    public void test3(JSONObject message){
        System.out.println(message);
    }

}
```

## 发布与订阅模式
发布者向某个channel发布消息，所有订阅了该channel的消费端均能接受消息

### 消费端

使用`@RedisSubscribeListener`修饰某个方法，即可定义一个消费端，其中`channel`表示要订阅的频道：
```java
import com.alibaba.fastjson.JSONObject;
import com.leng.project.redisqueue.annotation.RedisSubscribeListener;
import org.springframework.stereotype.Component;

@Component
public class ChannelConsumer {

    //带参数
    @RedisSubscribeListener(channel = "test_channel")
    public void test(JSONObject message) {
        System.out.println("test_channel:" + message);        
    }
    
    //不带参数
    @RedisSubscribeListener(channel = "test_channel_02")
    public void test() {
      System.out.println("test_channel_02");
    }
    
}
```

channel还可以使用通配符：
```java
import com.alibaba.fastjson.JSONObject;
import com.leng.project.redisqueue.annotation.RedisSubscribeListener;
import com.leng.project.redisqueue.utils.SubscribeUtils;
import org.springframework.stereotype.Component;

@Component
public class ChannelConsumer {
    
    @RedisSubscribeListener(channel = "test_channel*")
    public void test2() {
        System.out.println("test_channel*:" + SubscribeUtils.currentChannel());
    }
}
```
使用通配符后，如果想确切地知道是哪个channel产生的消息，可以使用`SubscribeUtils.currentChannel()`

### 生产者
```java
@Autowired
private RedisQueueTemplate redisQueueTemplate;

public String testSendChannelMessage(){
    JSONObject message = new JSONObject();
    message.put("bondId", "17f62f1dfb5afb12e8d67cd651c1df53");
    message.put("year", 2022);
    //带参数
    redisQueueTemplate.sendChannelMessage("test_channel", message);
    //不带参数
    redisQueueTemplate.sendChannelMessage("test_channel_02");
    return "OK";
}

```


## 虚拟空间
参考了RabbitMQ的设计。虚拟空间很有必要，例如，开发环境和测试环境的数据如果没有隔离，在调试时被测试环境的消费端干扰。
配置虚拟空间：
```yaml
queue:
  virtual-host: /dev  #默认为 /
```

## Web管理平台
浏览器访问：<http://ip:port/queue.html>，默认的账号密码为admin/admin

配置账号：
```yaml
queue:
  console:
    #是否启用web控制台
    enable: true
    username: admin #登录用户名
    password: 123456 #密码
```
登录成功后的界面，可查看所有虚拟空间的队列及消费情况：
![image.png](https://s2.loli.net/2024/02/22/9VJcGSTAbCFxZId.png)

注：如果你的系统使用了权限控制框架，如shiro、spring-security等，则需要对如下3个资源放行：
- /queue.html
- /queue/**
- /static/**
