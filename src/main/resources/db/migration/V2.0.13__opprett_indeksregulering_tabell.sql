CREATE TABLE IF NOT EXISTS indeksregulering
(
    id                    INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    batch_id              TEXT    NOT NULL,
    saksnummer            TEXT    NOT NULL,
    ar                   INTEGER NOT NULL,
    stonadstype           TEXT    NOT NULL,
    begrunnelse           TEXT[],
    status                TEXT    NOT NULL,
    gjennomfort           BOOLEAN NOT NULL DEFAULT FALSE,
    vedtak                INTEGER,
    opprettet_tidspunkt   TIMESTAMP DEFAULT current_timestamp,
    fattet_tidspunkt      TIMESTAMP,
    resultat_siste_vedtak TEXT,
    -- Forhindrer at det opprettes flere indeksregulering-rader for samme sak, stønadstype og år.
    CONSTRAINT indeksregulering_saksnummer_stonadstype_aar_unique UNIQUE (saksnummer, stonadstype, ar)
);

CREATE INDEX IF NOT EXISTS indeksregulering_status_index ON indeksregulering (status);
CREATE INDEX IF NOT EXISTS indeksregulering_stonadstype_index ON indeksregulering (stonadstype);
CREATE INDEX IF NOT EXISTS indeksregulering_gjennomfort_index ON indeksregulering (gjennomfort);

-- Join-tabell for many-to-many-relasjon mellom indeksregulering og barn.
-- Alle barn på samme sak ligger under samme indeksregulering-rad (én rad per sak).
CREATE TABLE IF NOT EXISTS indeksregulering_barn
(
    indeksregulering_id INTEGER NOT NULL REFERENCES indeksregulering (id) ON DELETE CASCADE,
    barn_id             INTEGER NOT NULL REFERENCES barn (id) ON DELETE CASCADE,
    PRIMARY KEY (indeksregulering_id, barn_id)
);

CREATE INDEX IF NOT EXISTS indeksregulering_barn_indeksregulering_id_index ON indeksregulering_barn (indeksregulering_id);
CREATE INDEX IF NOT EXISTS indeksregulering_barn_barn_id_index ON indeksregulering_barn (barn_id);
