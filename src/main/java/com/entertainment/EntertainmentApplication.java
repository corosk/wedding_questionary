package com.entertainment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class EntertainmentApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(EntertainmentApplication.class, args);
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EntertainmentApplication.class);
    }
}
    