package com.alia.nuts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

import javax.annotation.PostConstruct;

@EnableRabbit
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NutsApplicationServer implements InitializingBean, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger("CAST supervisor");

    public NutsApplicationServer() {
    }

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(20);
        return taskScheduler;
    }


    @Override
    public void afterPropertiesSet() {
    }

    @Override
    public void destroy() {
        logger.info("Nuts server stopped");
    }


    @PostConstruct
    private void postConstruct() {
        logger.info("starting");
        logger.info("os.name is " + System.getProperty("os.name"));
    }


    public static void main(String[] args) {
        SpringApplication.run(NutsApplicationServer.class, args);
    }

}
