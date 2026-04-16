package com.microservices.brokermessage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrokerMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerMessageApplication.class, args);
    }
}
