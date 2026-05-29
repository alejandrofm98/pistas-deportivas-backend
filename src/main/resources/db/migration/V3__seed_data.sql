-- Seed courts
INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000001', 'Pista Tenis Central', 'TENIS', 'Pista de tenis profesional con superficie de arcilla verde. Iluminación LED y gradas para espectadores.', 60, 25.00, 'https://images.pexels.com/photos/209977/pexels-photo-209977.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Pista Tenis Central');

INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000002', 'Campo Fútbol 7 Norte', 'FUTBOL', 'Campo de fútbol 7 con césped artificial de última generación.', 90, 67.50, 'https://images.pexels.com/photos/3148582/pexels-photo-3148582.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Campo Fútbol 7 Norte');

INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000003', 'Pista Pádel Premium', 'PADEL', 'Pista de pádel con cristal y malla. Superficie de hierba artificial.', 60, 30.00, 'https://images.pexels.com/photos/8612580/pexels-photo-8612580.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Pista Pádel Premium');

INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000004', 'Pista Baloncesto', 'BALONCESTO', 'Pista de baloncesto indoor con suelo de parquet profesional.', 60, 20.00, 'https://images.pexels.com/photos/1292864/pexels-photo-1292864.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Pista Baloncesto');

INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000005', 'Campo Fútbol 11 Sur', 'FUTBOL', 'Campo de fútbol 11 reglamentario con césped natural.', 120, 160.00, 'https://images.pexels.com/photos/47756/foothball-turf-goal-47756.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Campo Fútbol 11 Sur');

INSERT INTO courts (id, name, type, description, duration_minutes, price, image_url, is_active)
SELECT 'c1c1c1c1-0000-4000-8000-000000000006', 'Pista Voleibol Arena', 'VOLEIBOL', 'Pista de voleibol de arena profesional.', 60, 35.00, 'https://images.pexels.com/photos/1618180/pexels-photo-1618180.jpeg?auto=compress&cs=tinysrgb&w=800', TRUE
WHERE NOT EXISTS (SELECT 1 FROM courts WHERE name = 'Pista Voleibol Arena');

-- Seed amenities
INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Iluminación', 'Gradas', 'Vestuarios', 'Aparcamiento'])
FROM courts WHERE name = 'Pista Tenis Central'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Pista Tenis Central') AND amenity = 'Iluminación');

INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Césped artificial', 'Iluminación', 'Vestuarios', 'Duchas', 'Aparcamiento'])
FROM courts WHERE name = 'Campo Fútbol 7 Norte'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Campo Fútbol 7 Norte') AND amenity = 'Césped artificial');

INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Cristal', 'Iluminación', 'Vestuarios'])
FROM courts WHERE name = 'Pista Pádel Premium'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Pista Pádel Premium') AND amenity = 'Cristal');

INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Indoor', 'Suelo parquet', 'Marcador electrónico', 'Vestuarios'])
FROM courts WHERE name = 'Pista Baloncesto'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Pista Baloncesto') AND amenity = 'Indoor');

INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Césped natural', 'Tribuna', 'Iluminación', 'Vestuarios', 'Duchas', 'Aparcamiento'])
FROM courts WHERE name = 'Campo Fútbol 11 Sur'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Campo Fútbol 11 Sur') AND amenity = 'Césped natural');

INSERT INTO court_amenities (court_id, amenity)
SELECT id, unnest(ARRAY['Arena', 'Iluminación', 'Duchas exteriores'])
FROM courts WHERE name = 'Pista Voleibol Arena'
AND NOT EXISTS (SELECT 1 FROM court_amenities WHERE court_id = (SELECT id FROM courts WHERE name = 'Pista Voleibol Arena') AND amenity = 'Arena');
