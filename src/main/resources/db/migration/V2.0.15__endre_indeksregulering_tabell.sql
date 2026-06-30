CREATE TABLE IF NOT EXISTS indeksregulering
(
    id                    INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    batch_id              TEXT    NOT NULL,
    ar                   INTEGER NOT NULL,
    barn_id              INTEGER NOT NULL REFERENCES barn(id),
    stonadstype           TEXT    NOT NULL,
    begrunnelse           TEXT[],
    status                TEXT    NOT NULL,
    gjennomfort           BOOLEAN NOT NULL DEFAULT FALSE,
    vedtak                INTEGER,
    opprettet_tidspunkt   TIMESTAMP DEFAULT current_timestamp,
    fattet_tidspunkt      TIMESTAMP,
    -- Forhindrer at det opprettes flere indeksregulering-rader for samme barn, stønadstype og år.
    CONSTRAINT indeksregulering_barn_stonadstype_ar_unique UNIQUE (barn_id, stonadstype, ar)
);

CREATE INDEX IF NOT EXISTS indeksregulering_status_index ON indeksregulering (status);
CREATE INDEX IF NOT EXISTS indeksregulering_stonadstype_index ON indeksregulering (stonadstype);
CREATE INDEX IF NOT EXISTS indeksregulering_gjennomfort_index ON indeksregulering (gjennomfort);

ALTER TABLE barn
    ADD COLUMN bidrag_18_ar_fra DATE DEFAULT null,
    ADD COLUMN bidrag_18_ar_til DATE DEFAULT null,
    ADD COLUMN oppfostringsbidrag_fra DATE DEFAULT null,
    ADD COLUMN oppfostringsbidrag_til DATE DEFAULT null;

CREATE INDEX IF NOT EXISTS barn_bidrag_index
    ON barn (bidrag_fra, bidrag_til);

CREATE INDEX IF NOT EXISTS barn_bidrag_18_ar_index
    ON barn (bidrag_18_ar_fra, bidrag_18_ar_til);

CREATE INDEX IF NOT EXISTS barn_oppforstringsbidrag_index
    ON barn (oppfostringsbidrag_fra, oppfostringsbidrag_til);