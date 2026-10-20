-- SAMPLE DATA so the admin console matches docs/design/screens/A1–A5 (from source/AdminDashboard.dc.html).
-- Not needed in production: set FLYWAY_LOCATIONS=classpath:db/migration to skip this folder.
-- Sample accounts have no password (password_hash NULL), so nobody can sign in with them.
-- Phone numbers replace the design's "XXX" with 000; e-mails use the reserved example.com domain.
-- Pins are sample coordinates; the owner sets the real shop location in shop_settings.
-- Times use the shop time zone (Asia/Manila), as the backend does, because Supabase runs in UTC.

INSERT INTO users (role, full_name, email, mobile_number, password_hash, status, position, created_at, updated_at) VALUES
    ('CUSTOMER', 'Maria Santos',    'maria.santos@example.com',    '09170001021', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('CUSTOMER', 'Juan Dela Cruz',  'juan.delacruz@example.com',   '09180003340', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('CUSTOMER', 'Ana Reyes',       'ana.reyes@example.com',       '09270005582', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('CUSTOMER', 'Mark Villanueva', 'mark.villanueva@example.com', '09950007713', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('CUSTOMER', 'Liza Gonzaga',    'liza.gonzaga@example.com',    '09060002209', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('CUSTOMER', 'Paolo Tan',       'paolo.tan@example.com',       '09190006650', NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    -- A4 Riders tab shows "[RIDER 1]" / "[RIDER 2]" with "[PLATE NO.]", both On duty.
    ('RIDER',    'Rider 1',         'rider1@example.com',          NULL,          NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    ('RIDER',    'Rider 2',         'rider2@example.com',          NULL,          NULL, 'ACTIVE', NULL, (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila')),
    -- A4 Staff tab: "[STAFF NAME]", staff@laundryhub.ph, Counter staff.
    ('STAFF',    'Staff 1',         'staff@laundryhub.ph',         NULL,          NULL, 'ACTIVE', 'Counter staff', (now() AT TIME ZONE 'Asia/Manila'), (now() AT TIME ZONE 'Asia/Manila'));

INSERT INTO riders (user_id, plate_number, on_duty)
SELECT id, CASE email WHEN 'rider1@example.com' THEN 'SMP 0001' ELSE 'SMP 0002' END, TRUE
FROM users WHERE role = 'RIDER' AND email IN ('rider1@example.com', 'rider2@example.com');

-- ---------------------------------------------------------------------------
-- Orders LH-0001 … LH-0006 (A1 Recent orders, A2 list). Totals = kg × ₱30 + kg × soap + ₱60 delivery.
-- ---------------------------------------------------------------------------
INSERT INTO orders (order_no, customer_id, service_type, status, weight_kg, billable_kg, soap_id, soap_name,
                    rate_per_kg, soap_add_on_per_kg, laundry_cost, soap_cost, delivery_fee, total,
                    schedule_date, schedule_label, schedule_start, schedule_end,
                    pickup_latitude, pickup_longitude, pickup_address, cancelled_reason, created_at, updated_at)
SELECT o.order_no, u.id, o.service_type, o.status, o.kg, o.kg, s.id, s.name,
       30.00, s.add_on_per_kg, o.kg * 30.00, o.kg * s.add_on_per_kg, o.fee, o.kg * 30.00 + o.kg * s.add_on_per_kg + o.fee,
       CAST(now() AT TIME ZONE 'Asia/Manila' AS DATE) + o.day_offset, o.slot_label, o.slot_start, o.slot_end,
       o.lat, o.lng, o.address, NULL, (now() AT TIME ZONE 'Asia/Manila') - make_interval(mins => o.minutes_ago), (now() AT TIME ZONE 'Asia/Manila')
FROM (
    SELECT 'LH-0001' AS order_no, 'maria.santos@example.com' AS email, 'PICKUP_DELIVERY' AS service_type,
           'OUT_FOR_DELIVERY' AS status, 10.00 AS kg, 'Hypoallergenic' AS soap, 60.00 AS fee,
           0 AS day_offset, '2–4 PM' AS slot_label, TIME '14:00:00' AS slot_start, TIME '16:00:00' AS slot_end,
           14.609100 AS lat, 121.022300 AS lng, 'Brgy. San Isidro' AS address, 300 AS minutes_ago
    UNION ALL SELECT 'LH-0002', 'juan.delacruz@example.com', 'WALK_IN', 'WASHING', 8.00, 'Standard', 0.00,
           0, 'morning', NULL, NULL, NULL, NULL, NULL, 250
    UNION ALL SELECT 'LH-0003', 'ana.reyes@example.com', 'PICKUP_DELIVERY', 'PENDING', 12.00, 'Premium Scented', 60.00,
           0, '4–6 PM', TIME '16:00:00', TIME '18:00:00', 14.602700, 121.015600, 'Brgy. Poblacion', 200
    UNION ALL SELECT 'LH-0004', 'mark.villanueva@example.com', 'WALK_IN', 'READY', 9.00, 'Baby-Safe', 0.00,
           0, 'afternoon', NULL, NULL, NULL, NULL, NULL, 150
    UNION ALL SELECT 'LH-0005', 'liza.gonzaga@example.com', 'PICKUP_DELIVERY', 'COMPLETED', 8.00, 'Standard', 60.00,
           0, '9–11 AM', TIME '09:00:00', TIME '11:00:00', 14.614800, 121.030900, 'Brgy. Santa Cruz', 100
    UNION ALL SELECT 'LH-0006', 'paolo.tan@example.com', 'PICKUP_DELIVERY', 'PENDING', 15.00, 'Hypoallergenic', 60.00,
           1, '9–11 AM', TIME '09:00:00', TIME '11:00:00', 14.596400, 121.026100, 'Brgy. Bagong Silang', 50
) AS o
JOIN users u ON u.email = o.email
JOIN soaps s ON s.name = o.soap;

-- Payments: GCash orders are paid (A2 "Total · GCash (paid)"), cash orders are still unpaid.
INSERT INTO payments (order_id, method, amount, status, gcash_number, reference_no, collected_by_rider_id,
                      paid_at, created_at, updated_at)
SELECT o.id, p.method, o.total, p.status, p.gcash_number, p.reference_no, NULL,
       CASE p.status WHEN 'PAID' THEN o.created_at END, o.created_at, (now() AT TIME ZONE 'Asia/Manila')
FROM (
    SELECT 'LH-0001' AS order_no, 'GCASH' AS method, 'PAID' AS status, '09170001021' AS gcash_number, 'SAMPLE-0001' AS reference_no
    UNION ALL SELECT 'LH-0002', 'CASH',  'UNPAID', NULL,          NULL
    UNION ALL SELECT 'LH-0003', 'CASH',  'UNPAID', NULL,          NULL
    UNION ALL SELECT 'LH-0004', 'GCASH', 'PAID',   '09950007713', 'SAMPLE-0004'
    UNION ALL SELECT 'LH-0005', 'GCASH', 'PAID',   '09060002209', 'SAMPLE-0005'
    UNION ALL SELECT 'LH-0006', 'CASH',  'UNPAID', NULL,          NULL
) AS p
JOIN orders o ON o.order_no = p.order_no;

-- Status history up to each order's current status (timeline on 07).
INSERT INTO order_status_history (order_id, status, changed_by, note, changed_at)
SELECT o.id, h.status, NULL, NULL, o.created_at + make_interval(mins => h.step * 20)
FROM (
    SELECT 'LH-0001' AS order_no, 'PENDING' AS status, 0 AS step
    UNION ALL SELECT 'LH-0001', 'WASHING', 1
    UNION ALL SELECT 'LH-0001', 'READY', 2
    UNION ALL SELECT 'LH-0001', 'OUT_FOR_DELIVERY', 3
    UNION ALL SELECT 'LH-0002', 'PENDING', 0
    UNION ALL SELECT 'LH-0002', 'WASHING', 1
    UNION ALL SELECT 'LH-0003', 'PENDING', 0
    UNION ALL SELECT 'LH-0004', 'PENDING', 0
    UNION ALL SELECT 'LH-0004', 'WASHING', 1
    UNION ALL SELECT 'LH-0004', 'READY', 2
    UNION ALL SELECT 'LH-0005', 'PENDING', 0
    UNION ALL SELECT 'LH-0005', 'WASHING', 1
    UNION ALL SELECT 'LH-0005', 'READY', 2
    UNION ALL SELECT 'LH-0005', 'OUT_FOR_DELIVERY', 3
    UNION ALL SELECT 'LH-0005', 'COMPLETED', 4
    UNION ALL SELECT 'LH-0006', 'PENDING', 0
) AS h
JOIN orders o ON o.order_no = h.order_no;

-- Rider jobs: every Pickup & Delivery order has a PICKUP and a RETURN_DELIVERY job.
-- LH-0003 and LH-0006 pickups are open on R1 "Available bookings"; LH-0001 is on its way back.
INSERT INTO rider_jobs (order_id, job_type, status, rider_id, steps_completed, available_at, accepted_at,
                        completed_at, version, created_at, updated_at)
SELECT o.id, j.job_type, j.status, r.id, j.steps_completed,
       CASE WHEN j.status <> 'WAITING' THEN o.created_at END,
       CASE WHEN r.id IS NOT NULL THEN o.created_at + INTERVAL '5 minutes' END,
       CASE WHEN j.status = 'COMPLETED' THEN o.created_at + INTERVAL '40 minutes' END,
       0, o.created_at, (now() AT TIME ZONE 'Asia/Manila')
FROM (
    SELECT 'LH-0001' AS order_no, 'PICKUP' AS job_type, 'COMPLETED' AS status, 'rider1@example.com' AS rider_email, 4 AS steps_completed
    UNION ALL SELECT 'LH-0001', 'RETURN_DELIVERY', 'IN_PROGRESS', 'rider1@example.com', 2
    UNION ALL SELECT 'LH-0003', 'PICKUP',          'AVAILABLE',   NULL,                 0
    UNION ALL SELECT 'LH-0003', 'RETURN_DELIVERY', 'WAITING',     NULL,                 0
    UNION ALL SELECT 'LH-0005', 'PICKUP',          'COMPLETED',   'rider2@example.com', 4
    UNION ALL SELECT 'LH-0005', 'RETURN_DELIVERY', 'COMPLETED',   'rider2@example.com', 4
    UNION ALL SELECT 'LH-0006', 'PICKUP',          'AVAILABLE',   NULL,                 0
    UNION ALL SELECT 'LH-0006', 'RETURN_DELIVERY', 'WAITING',     NULL,                 0
) AS j
JOIN orders o ON o.order_no = j.order_no
LEFT JOIN users r ON r.email = j.rider_email;

-- Last GPS point of the rider delivering LH-0001 ("Rider is on the way · live GPS").
INSERT INTO rider_locations (rider_id, latitude, longitude, accuracy_meters, recorded_at)
SELECT id, 14.606500, 121.019800, 8.00, (now() AT TIME ZONE 'Asia/Manila') FROM users WHERE email = 'rider1@example.com';
