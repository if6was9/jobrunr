ALTER TABLE jobrunr_jobs
    ADD deadline DATETIME2;

CREATE INDEX jobrunr_job_carbon_aware_deadline_idx
    ON jobrunr_jobs (state, deadline ASC) WHERE state = 'AWAITING' AND deadline IS NOT NULL;