ALTER TABLE aldersjustering
    DROP COLUMN vedtakforsendelse_id,
    DROP COLUMN vedtakjournalpost_id;

DROP INDEX IF EXISTS aldersjustering_vedtakforsendelsId_vedtakjournalpostId_index;

CREATE TABLE IF NOT EXISTS forsendelse_bestilling
(
    id                    INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    aldersjustering_id    INTEGER NOT NULL REFERENCES aldersjustering (id),
    forsendelse_id        BIGINT,
    journalpost_id        BIGINT,
    rolletype             TEXT,
    mottaker              TEXT,
    sprakkode             TEXT,
    dokumentmal           TEXT,
    opprettet_tidspunkt   TIMESTAMP DEFAULT current_timestamp,
    bestilt_tidspunkt     TIMESTAMP,
    distribuert_tidspunkt TIMESTAMP,
    slettet_tidspunkt     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS forsendelse_id_index ON forsendelse_bestilling (forsendelse_id);
CREATE INDEX IF NOT EXISTS forsendelse_id_distribuer_tidspunkt_slettet_tidspunkt_index ON forsendelse_bestilling (forsendelse_id, distribuert_tidspunkt, slettet_tidspunkt);
CREATE INDEX IF NOT EXISTS bestilt_tidspunkt_slettet_tidspunkt_index ON forsendelse_bestilling (bestilt_tidspunkt, slettet_tidspunkt);
