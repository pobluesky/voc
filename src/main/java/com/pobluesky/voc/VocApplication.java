package com.pobluesky.voc;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.pobluesky.voc.global",
    "com.pobluesky.voc.answer",
    "com.pobluesky.voc.collaboration",
    "com.pobluesky.voc.question",
})
@EnableFeignClients(basePackages = "com.pobluesky.voc.feign")
public class VocApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocApplication.class, args);
    }

    @Bean
    public JPAQueryFactory init(EntityManager em) {
        return new JPAQueryFactory(em);
    }

}
