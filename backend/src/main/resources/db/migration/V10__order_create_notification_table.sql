-- Order & Prescription module (com.myopty.order)
-- V9 is applied and merged, so the notification story cannot edit it. This is a
-- new migration on the version map in CONTRIBUTION.md, announced in team chat
-- before applying.
--
-- The customer is told about their order by email, not by having to remember to
-- look at a page that would not tell them anything had changed. Every time an
-- order moves, the customer is owed an explanation of what happened to it.

CREATE TABLE order_notification (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT,

    -- The order this is about. NOT NULL because a notification that is not about
    -- an order is not something the shop produces: the workflow is the only
    -- source of them. There is deliberately no foreign key to progressive_order
    -- here. The order module owns both tables, so the link can be enforced, and a
    -- notification outliving the order it describes is a history problem rather
    -- than a referential one; a customer should still be able to read why their
    -- order was rejected after the order row itself has been tidied away. Add the
    -- constraint when the retention rule is decided rather than guessing now.
    order_id         BIGINT       NOT NULL,

    -- Copied from progressive_order.customer_id at the moment the order moved,
    -- rather than joined for at read time.
    --
    -- Nullable for two separate reasons, and both are real. The shared user table
    -- (V6__shared_create_app_user_table.sql) does not exist yet, so orders can be
    -- placed with no customer at all. And an order placed before the shared
    -- module lands keeps its null customer even after the id becomes known,
    -- because backfilling customer_id onto historical orders is a decision for
    -- whoever owns the data, not a side effect of adding a notifications table.
    -- Nullable and unconstrained for the same reason as on progressive_order:
    -- there is no table to point a foreign key at.
    customer_id      BIGINT       NULL,

    -- The step the order was in before it moved, and the one it is in now. Both
    -- are recorded rather than just the new one, so the history reads as
    -- "approved, then ready" instead of a list of states with no order, and so a
    -- message can be worded for the move rather than for the destination alone
    -- ("your order is ready" means something different to someone who was never
    -- told it was approved).
    --
    -- Constrained to the same vocabulary as progressive_order.status. Without the
    -- check, a typo in a status name would be stored happily and only surface
    -- when someone read the row; the order table already refuses the same values,
    -- and a notification that could disagree with the order it describes would be
    -- worse than no notification.
    from_status      VARCHAR(20)  NOT NULL,
    to_status        VARCHAR(20)  NOT NULL,

    -- The text the customer reads, rendered once when the order moved.
    --
    -- Stored rather than derived at read time so the wording a customer was
    -- actually sent cannot change under them: rewording "your order is ready" to
    -- "collect your order today" must not silently rewrite what past
    -- notifications said. A future channel can render its own text from the two
    -- status columns; this column is the copy that was already committed to.
    message          VARCHAR(500) NOT NULL,

    -- When the order moved, which is when this was written. The customer reads
    -- these newest first, so this is the column the index and the sort are built
    -- around. Not the order's created_at: that says when the order arrived, not
    -- when there was anything to tell.
    created_at       DATETIME(6)  NOT NULL,

    PRIMARY KEY (notification_id),

    -- Serves the customer-facing read, "everything about this customer's orders".
    -- Same column and same shape as idx_progressive_order_customer on the order
    -- table, because it answers the same question.
    INDEX idx_order_notification_customer (customer_id),

    -- Serves the client read, "what has happened to this order". This is the
    -- column that is never null, so it is the one that works even while
    -- customer_id is null on most rows.
    INDEX idx_order_notification_order (order_id),

    CONSTRAINT chk_order_notification_from_status
        CHECK (from_status IN ('PENDING_REVIEW', 'APPROVED', 'PROCESSING', 'READY', 'DISPATCHED', 'REJECTED')),

    CONSTRAINT chk_order_notification_to_status
        CHECK (to_status IN ('PENDING_REVIEW', 'APPROVED', 'PROCESSING', 'READY', 'DISPATCHED', 'REJECTED'))
);

-- No unique key on (order_id, to_status), though a status is only ever reached
-- once. Uniqueness would be a stronger statement about the workflow than the
-- database should be making on its own: the forward-only rule is enforced in
-- OrderStatus, and a constraint here would turn any future correction (re-sending
-- a notification for a step the shop got wrong) into a failed insert rather than
-- a second row the shop can see and delete.
