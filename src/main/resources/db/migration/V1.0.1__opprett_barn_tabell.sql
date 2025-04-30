CREATE TABLE IF NOT EXISTS barn
(
    id           integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    saksnummer   text NOT NULL,
    kravhaver    text NOT NULL,
    fodselsdato  date,
    skyldner     text,
    forskudd_fra date,
    forskudd_til date,
    bidrag_fra   date,
    bidrag_til   date,
    opprettet    timestamp DEFAULT current_timestamp
);

CREATE INDEX IF NOT EXISTS fodselsdato_index ON barn (fodselsdato);
CREATE INDEX IF NOT EXISTS fodselsdato_bidrag_index ON barn (fodselsdato, bidrag_fra, bidrag_til);
CREATE INDEX IF NOT EXISTS fodselsdato_forskudd_index ON barn (fodselsdato, forskudd_fra, forskudd_til);
