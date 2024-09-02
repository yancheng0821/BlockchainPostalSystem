package com.postal;

import com.postal.service.ParcelStatusScheduler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;


@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Application.class, args);
        // 启动定时任务
//        ParcelStatusScheduler scheduler = context.getBean(ParcelStatusScheduler.class);
//        scheduler.start();
    }
}


