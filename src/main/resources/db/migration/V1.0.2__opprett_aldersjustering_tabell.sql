CREATE TABLE IF NOT EXISTS aldersjustering
(
    id                   INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    batch_id             TEXT NOT NULL,
    vedtaksid_beregning  INTEGER NOT NULL,
    barn_id              INTEGER NOT NULL REFERENCES barn(id),
    aldersgruppe         NUMERIC NOT NULL CHECK (aldersgruppe IN (6, 11, 15)),
    lopende_belop        NUMERIC,
    begrunnelse          TEXT[],
    status               TEXT NOT NULL CHECK (status IN ('UBEHANDLET', 'TRUKKET', 'BEHANDLET', 'SLETTES', 'SLETTET', 'FEILET', 'FATTET')),
    behandlingstype      TEXT CHECK (behandlingstype IN ('INGEN', 'FATTET_FORSLAG', 'MANUELL', 'FEILET')),
    vedtak               INTEGER,
    oppgave              INTEGER,
    opprettet_tidspunkt  TIMESTAMP DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS aldersjustering_barn_id_index ON aldersjustering (barn_id);
CREATE INDEX IF NOT EXISTS aldersjustering_status_index ON aldersjustering (status);
CREATE INDEX IF NOT EXISTS aldersjustering_behandlingstype_index ON aldersjustering (behandlingstype);
CREATE UNIQUE INDEX IF NOT EXISTS aldersjustering_barn_id_aldersgruppe_unique_index ON aldersjustering (barn_id, aldersgruppe)