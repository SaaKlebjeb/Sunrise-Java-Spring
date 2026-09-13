package com.sunrise.lendingdesk.config;

import com.sunrise.lendingdesk.thirdparty.AuditLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AuditLog auditLog() {
        return new AuditLog("sunrise-library");
    }
}
