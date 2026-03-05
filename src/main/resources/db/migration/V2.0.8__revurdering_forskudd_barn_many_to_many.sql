-- Join table for many-to-many relationship mellom revurdering_forskudd og barn
CREATE TABLE IF NOT EXISTS revurdering_forskudd_barn
(
    revurdering_forskudd_id INTEGER NOT NULL REFERENCES revurdering_forskudd (id) ON DELETE CASCADE,
    barn_id                 INTEGER NOT NULL REFERENCES barn (id) ON DELETE CASCADE,
    PRIMARY KEY (revurdering_forskudd_id, barn_id)
);

CREATE INDEX IF NOT EXISTS revurdering_forskudd_barn_revurdering_id_index ON revurdering_forskudd_barn (revurdering_forskudd_id);
CREATE INDEX IF NOT EXISTS revurdering_forskudd_barn_barn_id_index ON revurdering_forskudd_barn (barn_id);

