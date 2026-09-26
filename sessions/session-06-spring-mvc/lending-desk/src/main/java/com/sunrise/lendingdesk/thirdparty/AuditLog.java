package com.sunrise.lendingdesk.thirdparty;

/**
 * Pretend this class arrived in a jar: it carries no annotation, and it may not be given one.
 * The only way it becomes a bean is the @Bean method in AuditConfig.
 */
public class AuditLog {

    private final String system;

    public AuditLog(String system) {
        this.system = system;
    }

    public void record(String line) {
        System.out.println("[audit " + system + "] " + line);
    }
}
