package com.bilalkaun.examples.jdbcsession.notimeadvancement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@SpringBootApplication
@EnableJdbcHttpSession
public class PreventSessionTimeoutAdvancementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreventSessionTimeoutAdvancementApplication.class, args);
    }

}
