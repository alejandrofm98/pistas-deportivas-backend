CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
ADD CONSTRAINT no_overlapping_active_reservations
EXCLUDE USING gist (
    court_id WITH =,
    date WITH =,
    int4range(
        (start_time * 2)::int,
        (end_time * 2)::int
    ) WITH &&
) WHERE (status <> 'CANCELLED');
