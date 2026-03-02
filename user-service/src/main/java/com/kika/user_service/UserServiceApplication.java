package com.kika.user_service;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(UserServiceApplication.class, args);
        String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
        System.out.println("✅ Active profiles: " + Arrays.toString(activeProfiles));


        String port = ctx.getEnvironment().getProperty("server.port");
        System.out.println("✅ Effective server.port: " + port);
    }

}
