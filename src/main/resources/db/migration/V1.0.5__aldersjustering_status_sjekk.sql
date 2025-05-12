alter table public.aldersjustering
    drop constraint aldersjustering_status_check;

alter table public.aldersjustering
    add constraint aldersjustering_status_check
        check (status = ANY
               (ARRAY ['UBEHANDLET'::text, 'TRUKKET'::text, 'BEHANDLET'::text, 'SLETTES'::text, 'SLETTET'::text, 'FEILET'::text, 'FATTET'::text, 'SIMULERT'::text]));

