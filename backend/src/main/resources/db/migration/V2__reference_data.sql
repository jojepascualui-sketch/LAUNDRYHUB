-- Reference data every installation needs. Values from docs/design/DESIGN_SPEC.md section 1
-- and the soap list in docs/design/source/Main.dc.html. Admin can change all of it on A3.
-- The first admin account is created by AdminAccountSeeder (password from ADMIN_DEFAULT_PASSWORD).

INSERT INTO soaps (name, description, add_on_per_kg, available, tint_hex, sort_order, created_at, updated_at) VALUES
    ('Standard',        'Everyday clean for regular clothes', 0.00, TRUE, '#E4EEF8', 1, LOCALTIMESTAMP(6), LOCALTIMESTAMP(6)),
    ('Hypoallergenic',  'Gentle on sensitive skin',           5.00, TRUE, '#E6F2EA', 2, LOCALTIMESTAMP(6), LOCALTIMESTAMP(6)),
    ('Premium Scented', 'Long-lasting fresh fragrance',       8.00, TRUE, '#F3E8F7', 3, LOCALTIMESTAMP(6), LOCALTIMESTAMP(6)),
    ('Baby-Safe',       'Mild and dye-free formula',          6.00, TRUE, '#FFF1E8', 4, LOCALTIMESTAMP(6), LOCALTIMESTAMP(6));

INSERT INTO pricing_settings (id, rate_per_kg, min_kg, delivery_fee, delivery_radius_km,
                              cash_enabled, gcash_enabled, shop_gcash_number, updated_at, updated_by)
VALUES (1, 30.00, 8.00, 60.00, 5.00, TRUE, TRUE, NULL, LOCALTIMESTAMP(6), NULL);

-- Address and hours are the design placeholders until the owner enters the real ones.
INSERT INTO shop_settings (id, shop_name, address, hours_label, latitude, longitude, contact_number, updated_at, updated_by)
VALUES (1, 'Laundry HUB', '[SHOP ADDRESS]', '[SHOP HOURS]', NULL, NULL, NULL, LOCALTIMESTAMP(6), NULL);
