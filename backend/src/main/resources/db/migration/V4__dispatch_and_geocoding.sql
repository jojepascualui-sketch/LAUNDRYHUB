-- Rider dispatch: the checklist position is called current_step (index of the next step to do,
-- which equals the number of steps already done). The CHECK is re-created because MySQL will not
-- rename a column that a CHECK constraint uses.
ALTER TABLE rider_jobs DROP CONSTRAINT ck_jobs_steps;
ALTER TABLE rider_jobs RENAME COLUMN steps_completed TO current_step;
ALTER TABLE rider_jobs ADD CONSTRAINT ck_jobs_current_step CHECK (current_step >= 0);

-- Reverse geocoding: the address Google returns for the pinned point (shown next to the
-- customer's own "Address details" on A2 and on the rider cards).
ALTER TABLE orders ADD COLUMN pickup_geocoded_address VARCHAR(255) NULL;
