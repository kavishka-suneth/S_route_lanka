SET @add_resend_available_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE otp_verifications ADD COLUMN resend_available_at DATETIME(6) NULL AFTER operation_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'otp_verifications'
      AND column_name = 'resend_available_at'
);
PREPARE stmt FROM @add_resend_available_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE otp_verifications
    MODIFY COLUMN resend_available_at DATETIME(6) NULL;
