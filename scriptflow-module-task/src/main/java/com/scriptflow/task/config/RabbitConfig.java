package com.scriptflow.task.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${scriptflow.rabbit.queue.task:scriptflow.task.submit}")
    private String taskQueue;

    @Value("${scriptflow.rabbit.queue.result:scriptflow.task.result}")
    private String resultQueue;

    @Value("${scriptflow.rabbit.queue.log:scriptflow.task.log}")
    private String logQueue;

    @Bean
    public Queue taskSubmitQueue() {
        return new Queue(taskQueue, true);
    }

    @Bean
    public Queue taskResultQueue() {
        return new Queue(resultQueue, true);
    }

    @Bean
    public Queue taskLogQueue() {
        return new Queue(logQueue, true);
    }
}
