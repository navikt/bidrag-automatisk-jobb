CREATE TABLE IF NOT EXISTS barn
(
    id           integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    saksnummer   text NOT NULL,
    kravhaver    text NOT NULL,
    fodselsdato  date NOT NULL,
    skyldner     text NOT NULL,
    forskudd_fra date NOT NULL,
    forskudd_til date,
    bidrag_fra   date NOT NULL,
    bidrag_til   date,
    opprettet    timestamp DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS fodselsdato_index ON barn (fodselsdato);
