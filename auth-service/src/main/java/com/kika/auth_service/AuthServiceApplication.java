package com.kika.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AuthServiceApplication.class, args);
        String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
        System.out.println("✅ Active profiles: " + Arrays.toString(activeProfiles));


        String port = ctx.getEnvironment().getProperty("server.port");
        System.out.println("✅ Effective server.port: " + port);
    }

}
