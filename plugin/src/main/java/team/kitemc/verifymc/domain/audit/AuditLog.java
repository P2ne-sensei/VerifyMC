package team.kitemc.verifymc.domain.audit;

public record AuditLog(Long id, String action, String operator, String target, String detail, long timestamp) {
    public AuditLog {
        action = action == null ? "" : action;
        operator = operator == null ? "" : operator;
        target = target == null ? "" : target;
        detail = detail == null ? "" : detail;
    }

    public AuditLog(String action, String operator, String target, String detail, long timestamp) {
        this(null, action, operator, target, detail, timestamp);
    }
}
