SET @schema_name = DATABASE();

SET @has_wallet_tx_reference = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'wallet_transactions'
      AND column_name = 'reference'
);

SET @sql = IF(
    @has_wallet_tx_reference = 1,
    'ALTER TABLE wallet_transactions MODIFY COLUMN reference VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
