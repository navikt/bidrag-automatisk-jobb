CREATE INDEX IF NOT EXISTS barn_forskudd_revurdering_index
    ON barn (id, forskudd_fra, forskudd_til)
    WHERE forskudd_fra IS NOT NULL;