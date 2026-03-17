-- Forhindrer at det opprettes flere revurdering_forskudd-rader for samme sak og måned.
-- Sikrer unikhet på databasenivå uavhengig av parallell batchkjøring.
ALTER TABLE revurdering_forskudd
    ADD CONSTRAINT revurdering_forskudd_saksnummer_for_maned_unique UNIQUE (saksnummer, for_maned);

