ALTER TABLE forsendelse_bestilling
    RENAME COLUMN bestilt_tidspunkt TO forsendelse_opprettet_tidspunkt;

alter table forsendelse_bestilling add column if not exists skal_slettes boolean not null default false;

CREATE INDEX idx_forsendelse_bestilling_opprett on forsendelse_bestilling (slettet_tidspunkt, forsendelse_id)
    WHERE slettet_tidspunkt IS NULL AND forsendelse_id IS NULL;

CREATE INDEX idx_forsendelse_bestilling_slett ON forsendelse_bestilling(skal_slettes, distribuert_tidspunkt, journalpost_id)
    WHERE skal_slettes IS TRUE AND distribuert_tidspunkt IS NULL AND journalpost_id IS NULL;