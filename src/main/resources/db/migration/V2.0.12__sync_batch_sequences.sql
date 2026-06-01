-- BATCH_JOB_INSTANCE_SEQ was created in V2.0.11 starting at 1, but the table already
-- contained rows inserted via the old BATCH_JOB_SEQ. Advance the sequence past the
-- current max so Spring Batch does not produce duplicate-key errors.
SELECT setval('batch_job_instance_seq', COALESCE((SELECT MAX(job_instance_id) FROM batch_job_instance), 0));
SELECT setval('batch_job_execution_seq', COALESCE((SELECT MAX(job_execution_id) FROM batch_job_execution), 0));
SELECT setval('batch_step_execution_seq', COALESCE((SELECT MAX(step_execution_id) FROM batch_step_execution), 0));
