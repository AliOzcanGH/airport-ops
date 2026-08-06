CREATE FUNCTION audit.reject_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit.audit_logs is append-only: % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_no_update
    BEFORE UPDATE ON audit.audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit.reject_audit_log_mutation();

CREATE TRIGGER trg_audit_logs_no_delete
    BEFORE DELETE ON audit.audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit.reject_audit_log_mutation();
