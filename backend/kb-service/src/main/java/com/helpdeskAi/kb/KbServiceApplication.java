package com.helpdeskAi.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication(scanBasePackages = {"com.helpdeskAi.kb", "com.helpdeskAi.common"})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class KbServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KbServiceApplication.class, args);
    }
}

