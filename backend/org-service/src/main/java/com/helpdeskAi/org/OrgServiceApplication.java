package com.helpdeskAi.org;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.helpdeskAi.org", "com.helpdeskAi.common"})
public class OrgServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
