-- CLEANUP
truncate table task_audit_log cascade;
truncate table task_links cascade;
truncate table tasks cascade;
truncate table employees cascade;
truncate table projects cascade;

alter sequence employees_id_seq restart with 1;
alter sequence projects_id_seq restart with 1;
alter sequence tasks_id_seq restart with 1;
alter sequence task_audit_log_id_seq restart with 1;
