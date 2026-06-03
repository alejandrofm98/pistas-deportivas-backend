ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_payment_method_check;

ALTER TABLE reservations ADD CONSTRAINT reservations_payment_method_check
    CHECK (payment_method IN ('ONLINE', 'BIZUM', 'ONSITE'));
