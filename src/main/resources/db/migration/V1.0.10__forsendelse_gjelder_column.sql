alter table forsendelse_bestilling add column if not exists gjelder text;
alter table forsendelse_bestilling add column if not exists feil_begrunnelse text;
