-- Order & Prescription module (com.myopty.order)
-- V8 is applied and merged, so the order rejection story cannot edit it. This is
-- a new migration on the version map in CONTRIBUTION.md, announced in team chat
-- before applying.

-- The client story is "approve or reject a progressive lens order", but
-- V8__order_create_progressive_order_table.sql only listed the happy path
-- (PENDING_REVIEW -> APPROVED -> PROCESSING -> READY -> DISPATCHED). A rejected
-- order had nowhere to go, so this adds the state the story needs.
--
-- Rejection is recorded on the order rather than left implicit in "never
-- advances", for the same reason the prescription table carries a
-- rejection_reason: a client that turns work down is a normal outcome, and a
-- status that cannot represent it forces the outcome to be a silent omission.

ALTER TABLE progressive_order
    -- Why the order was rejected, e.g. "stock unavailable for this frame".
    -- Nullable because only REJECTED orders carry one, and a rejection reason
    -- would be noise on every order that is progressing normally. Length
    -- mirrors prescription.rejection_reason so both tables speak the same size.
    ADD COLUMN rejection_reason VARCHAR(500) NULL;

-- MySQL has no ALTER CONSTRAINT, so the status check is dropped and replaced
-- with REJECTED added. The drop is required rather than optional: a second
-- constraint of the same name cannot be added alongside the first, and leaving
-- the old one in place would keep rejecting every REJECTED row this story tries
-- to write.
--
-- The forward path from V8 is unchanged. REJECTED is terminal: a client that
-- rejects an order by mistake has no way back to PENDING_REVIEW, which is
-- deliberate. A one-way decision keeps the status history unambiguous, and
-- un-rejecting an order is a separate story rather than a flag on this one.
ALTER TABLE progressive_order
    DROP CHECK chk_progressive_order_status;

ALTER TABLE progressive_order
    ADD CONSTRAINT chk_progressive_order_status
        CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'PROCESSING', 'READY', 'DISPATCHED', 'REJECTED'));
