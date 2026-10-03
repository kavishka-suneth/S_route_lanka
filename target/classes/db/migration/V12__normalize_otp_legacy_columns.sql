SET @add_operation_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE otp_verifications ADD COLUMN operation_id VARCHAR(120) NULL AFTER operation_key',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'otp_verifications'
      AND column_name = 'operation_id'
);
PREPARE stmt FROM @add_operation_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE otp_verifications
    MODIFY COLUMN operation_id VARCHAR(120) NULL;
