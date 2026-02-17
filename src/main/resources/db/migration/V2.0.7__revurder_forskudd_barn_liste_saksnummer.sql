ALTER TABLE revurdering_forskudd
    DROP COLUMN IF EXISTS barn_id,
    ADD COLUMN IF NOT EXISTS saksnummer text;