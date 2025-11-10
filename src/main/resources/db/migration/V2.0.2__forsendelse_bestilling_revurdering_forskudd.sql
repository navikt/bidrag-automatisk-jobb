ALTER TABLE forsendelse_bestilling
    DROP COLUMN aldersjustering_id,
    ADD COLUMN forsendelsestype TEXT,
    ADD COLUMN unik_referanse   TEXT,
    ADD COLUMN vedtak           INT,
    ADD COLUMN stonadstype      TEXT,
    ADD COLUMN barn_id          INT REFERENCES barn (id),
    ADD COLUMN batch_id         TEXT;


ALTER TABLE revurdering_forskudd
    ADD COLUMN stonadstype               TEXT,
    ADD COLUMN forsendelse_bestilling_id INT REFERENCES forsendelse_bestilling (id);

ALTER TABLE aldersjustering
    ADD COLUMN forsendelse_bestilling_id INT REFERENCES forsendelse_bestilling (id);
