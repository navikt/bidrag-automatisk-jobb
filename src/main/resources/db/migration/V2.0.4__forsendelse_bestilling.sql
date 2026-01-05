ALTER TABLE revurdering_forskudd
    DROP COLUMN forsendelse_bestilling_id;

ALTER TABLE aldersjustering
    DROP COLUMN forsendelse_bestilling_id;

ALTER TABLE forsendelse_bestilling
    ADD COLUMN aldersjustering_id      INTEGER REFERENCES aldersjustering (id),
    ADD COLUMN revurdering_forskudd_id INTEGER REFERENCES revurdering_forskudd (id);

