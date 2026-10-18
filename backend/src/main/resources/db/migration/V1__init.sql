-- Laundry HUB schema (MySQL 8). Field sources: docs/design/DESIGN_SPEC.md sections 1 and 3.
-- Money is DECIMAL(10,2), coordinates DECIMAL(9,6), timestamps DATETIME(6) in shop local time.

-- ---------------------------------------------------------------------------
-- Accounts. One table for every role (JPA single-table inheritance on `role`).
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    role           VARCHAR(20)  NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(190) NOT NULL,
    mobile_number  VARCHAR(11)  NULL COMMENT '09XXXXXXXXX (sign-up, A2 customer phone, A4 Contact)',
    password_hash  VARCHAR(100) NULL COMMENT 'BCrypt; NULL = account cannot sign in yet',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'A4 Status badge',
    position       VARCHAR(60)  NULL COMMENT 'Admin/staff role label in A4, e.g. Owner, Counter staff',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'RIDER', 'ADMIN', 'STAFF')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_users_mobile CHECK (mobile_number IS NULL OR mobile_number REGEXP '^09[0-9]{9}$')
);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_mobile ON users (mobile_number);

-- Rider-only fields (JPA secondary table of the Rider subclass).
CREATE TABLE riders (
    user_id       BIGINT      NOT NULL,
    plate_number  VARCHAR(20) NOT NULL COMMENT 'Tracking "[PLATE NO.]", A4 Vehicle column',
    on_duty       BOOLEAN     NOT NULL DEFAULT FALSE COMMENT 'R1/R5 On duty / Off duty switch',
    PRIMARY KEY (user_id),
    CONSTRAINT uk_riders_plate UNIQUE (plate_number),
    CONSTRAINT fk_riders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_riders_on_duty ON riders (on_duty);

-- ---------------------------------------------------------------------------
-- Catalog and settings (editable by admin on A3).
-- ---------------------------------------------------------------------------
CREATE TABLE soaps (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    name           VARCHAR(60)   NOT NULL,
    description    VARCHAR(120)  NOT NULL COMMENT 'Soap card text on screen 03',
    add_on_per_kg  DECIMAL(10,2) NOT NULL COMMENT '0 is shown as "Included"',
    available      BOOLEAN       NOT NULL DEFAULT TRUE COMMENT 'A3 Available checkbox',
    tint_hex       VARCHAR(7)    NOT NULL DEFAULT '#E4EEF8' COMMENT 'Icon tile color on screen 03',
    sort_order     INT           NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_soaps_name UNIQUE (name),
    CONSTRAINT ck_soaps_add_on CHECK (add_on_per_kg >= 0),
    CONSTRAINT ck_soaps_tint CHECK (tint_hex REGEXP '^#[0-9A-Fa-f]{6}$')
);

-- Single row (id = 1): rates, minimum load, fees, radius and payment methods.
CREATE TABLE pricing_settings (
    id                  BIGINT        NOT NULL,
    rate_per_kg         DECIMAL(10,2) NOT NULL,
    min_kg              DECIMAL(6,2)  NOT NULL,
    delivery_fee        DECIMAL(10,2) NOT NULL,
    delivery_radius_km  DECIMAL(5,2)  NOT NULL COMMENT 'Pins outside this distance from the shop are rejected',
    cash_enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    gcash_enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    shop_gcash_number   VARCHAR(11)   NULL,
    updated_at          DATETIME(6)   NOT NULL,
    updated_by          BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_pricing_single_row CHECK (id = 1),
    CONSTRAINT ck_pricing_rate CHECK (rate_per_kg > 0),
    CONSTRAINT ck_pricing_min_kg CHECK (min_kg > 0),
    CONSTRAINT ck_pricing_fee CHECK (delivery_fee >= 0),
    CONSTRAINT ck_pricing_radius CHECK (delivery_radius_km > 0),
    CONSTRAINT ck_pricing_gcash_number CHECK (shop_gcash_number IS NULL OR shop_gcash_number REGEXP '^09[0-9]{9}$'),
    CONSTRAINT fk_pricing_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Single row (id = 1): shop card on 04b and the shop marker on 07.
CREATE TABLE shop_settings (
    id              BIGINT        NOT NULL,
    shop_name       VARCHAR(80)   NOT NULL,
    address         VARCHAR(255)  NOT NULL COMMENT '[SHOP ADDRESS]',
    hours_label     VARCHAR(80)   NOT NULL COMMENT '[SHOP HOURS], shown as "Open <hours>"',
    latitude        DECIMAL(9,6)  NULL,
    longitude       DECIMAL(9,6)  NULL,
    contact_number  VARCHAR(11)   NULL,
    updated_at      DATETIME(6)   NOT NULL,
    updated_by      BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_shop_single_row CHECK (id = 1),
    CONSTRAINT ck_shop_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CONSTRAINT fk_shop_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------------
-- Orders. Prices are snapshots taken when the order is placed (server-calculated).
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    order_no            VARCHAR(20)   NOT NULL COMMENT 'LH-0001',
    customer_id         BIGINT        NOT NULL,
    service_type        VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    weight_kg           DECIMAL(6,2)  NOT NULL COMMENT 'Requested, later the weighed kilos',
    billable_kg         DECIMAL(6,2)  NOT NULL COMMENT 'max(weight_kg, minimum load)',
    soap_id             BIGINT        NOT NULL,
    soap_name           VARCHAR(60)   NOT NULL,
    rate_per_kg         DECIMAL(10,2) NOT NULL,
    soap_add_on_per_kg  DECIMAL(10,2) NOT NULL,
    laundry_cost        DECIMAL(10,2) NOT NULL,
    soap_cost           DECIMAL(10,2) NOT NULL,
    delivery_fee        DECIMAL(10,2) NOT NULL,
    total               DECIMAL(10,2) NOT NULL,
    schedule_date       DATE          NOT NULL COMMENT 'Pickup time (delivery) or drop-off time (walk-in)',
    schedule_label      VARCHAR(40)   NOT NULL COMMENT 'Slot text after "Today, " / "Tomorrow, ", e.g. 2–4 PM, morning',
    schedule_start      TIME          NULL,
    schedule_end        TIME          NULL,
    pickup_latitude     DECIMAL(9,6)  NULL COMMENT 'Pinned location (delivery only)',
    pickup_longitude    DECIMAL(9,6)  NULL,
    pickup_address      VARCHAR(255)  NULL COMMENT 'Address details: house no., street, barangay, landmark',
    cancelled_reason    VARCHAR(255)  NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_orders_soap FOREIGN KEY (soap_id) REFERENCES soaps (id),
    CONSTRAINT ck_orders_service CHECK (service_type IN ('WALK_IN', 'PICKUP_DELIVERY')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'WASHING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_orders_weight CHECK (weight_kg > 0 AND billable_kg >= weight_kg),
    CONSTRAINT ck_orders_money CHECK (laundry_cost >= 0 AND soap_cost >= 0 AND delivery_fee >= 0
                                      AND total = laundry_cost + soap_cost + delivery_fee),
    CONSTRAINT ck_orders_pin CHECK (service_type = 'WALK_IN'
                                    OR (pickup_latitude IS NOT NULL AND pickup_longitude IS NOT NULL)),
    CONSTRAINT ck_orders_schedule CHECK ((schedule_start IS NULL) = (schedule_end IS NULL)
                                         AND (schedule_start IS NULL OR schedule_start < schedule_end))
);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at);
CREATE INDEX idx_orders_created ON orders (created_at);

-- Timeline on 07 and the audit of status buttons on A2.
CREATE TABLE order_status_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    changed_by  BIGINT       NULL,
    note        VARCHAR(255) NULL,
    changed_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_history_status CHECK (status IN ('PENDING', 'WASHING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_history_order_changed ON order_status_history (order_id, changed_at);

-- One payment per order (JPA single-table inheritance on `method`).
CREATE TABLE payments (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    order_id               BIGINT        NOT NULL,
    method                 VARCHAR(10)   NOT NULL,
    amount                 DECIMAL(10,2) NOT NULL,
    status                 VARCHAR(10)   NOT NULL DEFAULT 'UNPAID' COMMENT 'A2 "(paid)/(unpaid)", R1 "Paid via GCash"',
    gcash_number           VARCHAR(11)   NULL COMMENT 'Customer GCash mobile number (05)',
    reference_no           VARCHAR(40)   NULL,
    collected_by_rider_id  BIGINT        NULL COMMENT 'Rider who collected cash on return delivery (R3)',
    paid_at                DATETIME(6)   NULL,
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_rider FOREIGN KEY (collected_by_rider_id) REFERENCES riders (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH', 'GCASH')),
    CONSTRAINT ck_payments_status CHECK (status IN ('UNPAID', 'PAID', 'REFUNDED')),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0),
    CONSTRAINT ck_payments_gcash CHECK (method <> 'GCASH'
                                        OR (gcash_number IS NOT NULL AND gcash_number REGEXP '^09[0-9]{9}$')),
    CONSTRAINT ck_payments_paid_at CHECK ((status = 'UNPAID') = (paid_at IS NULL))
);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_method ON payments (method);

-- ---------------------------------------------------------------------------
-- Riders: jobs offered to on-duty riders, declines, and live GPS points.
-- ---------------------------------------------------------------------------
CREATE TABLE rider_jobs (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    order_id         BIGINT      NOT NULL,
    job_type         VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL COMMENT 'WAITING = return delivery before the order is READY',
    rider_id         BIGINT      NULL,
    steps_completed  INT         NOT NULL DEFAULT 0 COMMENT 'Checklist progress on R2/R3',
    available_at     DATETIME(6) NULL,
    accepted_at      DATETIME(6) NULL,
    completed_at     DATETIME(6) NULL,
    version          BIGINT      NOT NULL DEFAULT 0 COMMENT 'Optimistic lock: only one rider can accept',
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_jobs_order_type UNIQUE (order_id, job_type),
    CONSTRAINT fk_jobs_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_rider FOREIGN KEY (rider_id) REFERENCES riders (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_jobs_type CHECK (job_type IN ('PICKUP', 'RETURN_DELIVERY')),
    CONSTRAINT ck_jobs_status CHECK (status IN ('WAITING', 'AVAILABLE', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_jobs_steps CHECK (steps_completed >= 0)
);
CREATE INDEX idx_jobs_status_type ON rider_jobs (status, job_type);
CREATE INDEX idx_jobs_rider_status ON rider_jobs (rider_id, status);

-- A decline hides the job from that rider only.
CREATE TABLE rider_job_declines (
    job_id       BIGINT      NOT NULL,
    rider_id     BIGINT      NOT NULL,
    declined_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (job_id, rider_id),
    CONSTRAINT fk_declines_job FOREIGN KEY (job_id) REFERENCES rider_jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_declines_rider FOREIGN KEY (rider_id) REFERENCES riders (user_id) ON DELETE CASCADE
);
CREATE INDEX idx_declines_rider ON rider_job_declines (rider_id);

-- Live GPS for "Rider is on the way · live GPS" (07) and the rider marker.
CREATE TABLE rider_locations (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    rider_id         BIGINT       NOT NULL,
    latitude         DECIMAL(9,6) NOT NULL,
    longitude        DECIMAL(9,6) NOT NULL,
    accuracy_meters  DECIMAL(7,2) NULL,
    recorded_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_locations_rider FOREIGN KEY (rider_id) REFERENCES riders (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_locations_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_locations_lng CHECK (longitude BETWEEN -180 AND 180)
);
CREATE INDEX idx_locations_rider_time ON rider_locations (rider_id, recorded_at);
