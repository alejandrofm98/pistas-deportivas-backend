-- Migration: Replace price_per_hour with duration_minutes and price
-- Idempotent: works on fresh DB (V1 already has columns) and existing DB (with price_per_hour)
-- Also cleans up old max_players column if present

ALTER TABLE courts ADD COLUMN IF NOT EXISTS duration_minutes INTEGER;
ALTER TABLE courts ADD COLUMN IF NOT EXISTS price DECIMAL(10,2);

-- Migrate data from old column if it still exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'courts' AND column_name = 'price_per_hour'
    ) THEN
        UPDATE courts SET duration_minutes = 60, price = price_per_hour
        WHERE price_per_hour IS NOT NULL AND duration_minutes IS NULL;
    END IF;
END $$;

ALTER TABLE courts ALTER COLUMN duration_minutes SET NOT NULL;
ALTER TABLE courts ALTER COLUMN price SET NOT NULL;

ALTER TABLE courts DROP COLUMN IF EXISTS price_per_hour;
ALTER TABLE courts DROP COLUMN IF EXISTS max_players;
