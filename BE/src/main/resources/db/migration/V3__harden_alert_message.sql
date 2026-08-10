UPDATE alerts
SET message = 'Notification'
WHERE message IS NULL OR btrim(message) = '';

ALTER TABLE alerts
    ALTER COLUMN message SET NOT NULL,
    ADD CONSTRAINT chk_alerts_message_not_blank CHECK (btrim(message) <> '');
