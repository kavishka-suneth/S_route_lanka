SET @schema_name = DATABASE();

SET @has_wallet_tx_user_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'wallet_transactions'
      AND column_name = 'user_id'
);

SET @sql = IF(
    @has_wallet_tx_user_id = 1,
    'ALTER TABLE wallet_transactions MODIFY COLUMN user_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
