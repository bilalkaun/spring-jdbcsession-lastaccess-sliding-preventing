package com.bilalkaun.examples.jdbcsession.notimeadvancement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@SpringBootApplication
// flushMode MUST be ON_SAVE
@EnableJdbcHttpSession(flushMode = FlushMode.ON_SAVE)
public class PreventSessionTimeoutAdvancementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreventSessionTimeoutAdvancementApplication.class, args);
    }

}
