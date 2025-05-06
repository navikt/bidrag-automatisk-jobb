ALTER TABLE aldersjustering
    ALTER COLUMN vedtakforsendelse_id TYPE BIGINT,
    ALTER COLUMN vedtakjournalpost_id TYPE BIGINT;

DROP INDEX IF EXISTS aldersjustering_vedtakforsendelsId_vedtakjournalpostId_index;
CREATE INDEX IF NOT EXISTS aldersjustering_vedtakforsendelsId_vedtakjournalpostId_index
    ON aldersjustering (vedtakforsendelse_id, vedtakjournalpost_id);