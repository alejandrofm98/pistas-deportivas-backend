-- V4 replaces this constraint to add FRONTON. Fresh installs need the
-- constraint first because V4 drops it unconditionally.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'courts_type_check'
          AND conrelid = 'courts'::regclass
    ) THEN
        ALTER TABLE courts
            ADD CONSTRAINT courts_type_check
            CHECK (type IN ('TENIS', 'FUTBOL', 'PADEL', 'BALONCESTO', 'VOLEIBOL'));
    END IF;
END
$$;
