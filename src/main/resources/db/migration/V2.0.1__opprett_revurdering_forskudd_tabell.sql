CREATE TABLE IF NOT EXISTS revurdering_forskudd
(
    id                    INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    for_maned             TEXT    NOT NULL,
    batch_id              TEXT    NOT NULL,
    barn_id               INTEGER NOT NULL REFERENCES barn (id),
    begrunnelse           TEXT[],
    status                TEXT    NOT NULL,
    behandlingstype       TEXT,
    vedtaksid_beregning   INTEGER,
    vedtak                INTEGER,
    oppgave               INTEGER,
    opprettet_tidspunkt   TIMESTAMP DEFAULT current_timestamp,
    fattet_tidspunkt      TIMESTAMP,
    resultat_siste_vedtak TEXT
);

CREATE INDEX IF NOT EXISTS revurdering_forskudd_barn_id_index ON revurdering_forskudd (barn_id);
CREATE INDEX IF NOT EXISTS revurdering_forskudd_status_index ON revurdering_forskudd (status);
CREATE INDEX IF NOT EXISTS revurdering_forskudd_behandlingstype_index ON revurdering_forskudd (behandlingstype);
CREATE UNIQUE INDEX IF NOT EXISTS aldersjustering_barn_id_aldersgruppe_unique_index ON revurdering_forskudd (barn_id, for_maned)