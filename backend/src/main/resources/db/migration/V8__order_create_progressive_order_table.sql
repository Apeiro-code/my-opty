-- Order & Prescription module (com.myopty.order)
-- Version 8 continues the version map agreed in CONTRIBUTION.md: V2 catalog,
-- V3 order, V4 workflow, V5 billing, V6 shared, V7 catalog, so the order
-- module's next free version is V8. Announce it in team chat before applying.
--
-- The table is named progressive_order rather than order because ORDER is a
-- reserved word in SQL and would have to be quoted everywhere. It matches the
-- module EER diagram, which also models the 1:1 link to a prescription.

CREATE TABLE progressive_order (
    order_id        BIGINT       NOT NULL AUTO_INCREMENT,
    -- Owned by the shared module (V6__shared_create_app_user_table.sql).
    -- Kept as a plain column for the same reason as on `prescription`: the
    -- shared table does not exist yet, so a foreign key cannot be declared.
    customer_id     BIGINT       NULL,
    -- The prescription this order is built from. NOT NULL because the module
    -- EER requires exactly one prescription per progressive order, and UNIQUE
    -- because that prescription may only ever be ordered once: without it two
    -- concurrent submissions would both pass the application's pre-check and
    -- the second insert would silently create a duplicate production job.
    prescription_id BIGINT       NOT NULL,
    -- Owned by the catalog module
    -- (V2__catalog_create_frame_table.sql, V7__catalog_add_frame_image_url.sql).
    -- Nullable and unconstrained because neither catalog table exists yet, so
    -- there is nothing to point a foreign key at and nothing that could confirm
    -- the id resolves. Add the foreign keys when the catalog owner lands V2/V7.
    frame_id        BIGINT       NULL,
    lens_id         BIGINT       NULL,
    -- The order type the customer selected. It decides which workflow the
    -- order enters, which is the whole point of asking for it rather than
    -- inferring it. Mirrors OrderType.
    order_type      VARCHAR(20)  NOT NULL,
    -- PENDING_REVIEW | APPROVED | PROCESSING | READY | DISPATCHED, the workflow in
    -- the module documentation. Always written as PENDING_REVIEW for now: the story
    -- that moves an order along belongs to the client, not the customer.
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW',
    -- Filled in when the order is approved and the estimate is calculated. Left
    -- null here so a column the estimate will own does not get a guess in it.
    receive_date    DATE         NULL,
    -- Stands in for the order_date of the EER diagram: the instant the customer
    -- placed the order is the only one that exists at creation time.
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_progressive_order_prescription (prescription_id),
    INDEX idx_progressive_order_customer (customer_id),
    INDEX idx_progressive_order_type (order_type),
    INDEX idx_progressive_order_status (status),
    -- The enum constants are asserted against these two lists by
    -- OrderTypeTest, so a constant added without a migration fails the build.
    CONSTRAINT chk_progressive_order_type CHECK (order_type IN ('SINGLE_VISION', 'BIFOCAL', 'PROGRESSIVE')),
    CONSTRAINT chk_progressive_order_status CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'PROCESSING', 'READY', 'DISPATCHED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
