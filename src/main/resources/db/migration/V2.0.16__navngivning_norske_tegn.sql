ALTER TABLE aldersjustering RENAME COLUMN lopende_belop TO løpende_beløp;
ALTER TABLE aldersjustering RENAME COLUMN stonadstype TO stønadstype;
ALTER TABLE aldersjustering RENAME COLUMN b4_belop TO b4_beløp;

ALTER TABLE indeksregulering RENAME COLUMN ar TO år;
ALTER TABLE indeksregulering RENAME COLUMN stonadstype TO stønadstype;
ALTER TABLE indeksregulering DROP COLUMN gjennomfort;
ALTER TABLE indeksregulering RENAME COLUMN belop TO beløp;
ALTER TABLE indeksregulering RENAME CONSTRAINT indeksregulering_barn_stonadstype_ar_unique TO indeksregulering_barn_stønadstype_år_unique;

ALTER TABLE forsendelse_bestilling RENAME COLUMN sprakkode TO språkkode;
ALTER TABLE forsendelse_bestilling RENAME COLUMN stonadstype TO stønadstype;

ALTER TABLE barn RENAME COLUMN fodselsdato TO fødselsdato;
ALTER TABLE barn RENAME COLUMN bidrag_18_ar_fra TO bidrag_18_år_fra;
ALTER TABLE barn RENAME COLUMN bidrag_18_ar_til TO bidrag_18_år_til;

ALTER TABLE revurdering_forskudd RENAME COLUMN for_maned TO for_måned;
ALTER TABLE revurdering_forskudd RENAME COLUMN stonadstype TO stønadstype;
