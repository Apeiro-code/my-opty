-- Order & Prescription module (com.myopty.order)
-- Version 3 follows the version map agreed in CONTRIBUTION.md
-- (V2 catalog, V3 order, V4 workflow, V5 billing, V6 shared).
--
-- Optical values follow ISO 8594-1: sphere/cylinder/addition in dioptres
-- (0.25 steps), axis in whole degrees. Values are stored as DECIMAL so the
-- 0.25 quarter-step is never rounded away.

CREATE TABLE prescription (
    prescription_id     BIGINT       NOT NULL AUTO_INCREMENT,
    -- Owned by the shared module (V6__shared_create_app_user_table.sql).
    -- Kept as a plain column: the shared table does not exist yet, so a
    -- foreign key cannot be declared. Add it when V6 lands.
    customer_id         BIGINT       NULL,
    is_progressive      BOOLEAN      NOT NULL DEFAULT FALSE,
    issued_date         DATE         NULL,
    -- Right eye (oculus dexter)
    sph_right           DECIMAL(5,2) NULL,
    cyl_right           DECIMAL(5,2) NULL,
    axis_right          SMALLINT     NULL,
    add_right           DECIMAL(5,2) NULL,
    -- Left eye (oculus sinister)
    sph_left            DECIMAL(5,2) NULL,
    cyl_left            DECIMAL(5,2) NULL,
    axis_left           SMALLINT     NULL,
    add_left            DECIMAL(5,2) NULL,
    notes               VARCHAR(500) NULL,
    -- PENDING_REVIEW | VERIFIED | REJECTED (set by the client, story "client reviews prescription")
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason    VARCHAR(500) NULL,
    -- Uploaded prescription document (scan or photo), stored in MinIO.
    -- object_key is private; the original filename is kept only for display.
    document_object_key VARCHAR(255) NULL,
    document_filename   VARCHAR(255) NULL,
    document_content_type VARCHAR(100) NULL,
    document_size_bytes BIGINT       NULL,
    document_uploaded_at DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (prescription_id),
    INDEX idx_prescription_customer (customer_id),
    INDEX idx_prescription_status (status),
    CONSTRAINT chk_prescription_sph_right CHECK (sph_right IS NULL OR sph_right BETWEEN -30.00 AND 30.00),
    CONSTRAINT chk_prescription_sph_left CHECK (sph_left IS NULL OR sph_left BETWEEN -30.00 AND 30.00),
    CONSTRAINT chk_prescription_cyl_right CHECK (cyl_right IS NULL OR cyl_right BETWEEN -10.00 AND 10.00),
    CONSTRAINT chk_prescription_cyl_left CHECK (cyl_left IS NULL OR cyl_left BETWEEN -10.00 AND 10.00),
    CONSTRAINT chk_prescription_axis_right CHECK (axis_right IS NULL OR axis_right BETWEEN 0 AND 180),
    CONSTRAINT chk_prescription_axis_left CHECK (axis_left IS NULL OR axis_left BETWEEN 0 AND 180),
    CONSTRAINT chk_prescription_add_right CHECK (add_right IS NULL OR add_right BETWEEN 0.00 AND 4.00),
    CONSTRAINT chk_prescription_add_left CHECK (add_left IS NULL OR add_left BETWEEN 0.00 AND 4.00)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
