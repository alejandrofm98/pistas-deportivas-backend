ALTER TABLE courts DROP CONSTRAINT courts_type_check;

ALTER TABLE courts ADD CONSTRAINT courts_type_check
    CHECK (((type)::text = ANY ((ARRAY['TENIS'::character varying, 'FUTBOL'::character varying, 'PADEL'::character varying, 'BALONCESTO'::character varying, 'VOLEIBOL'::character varying, 'FRONTON'::character varying])::text[])));
