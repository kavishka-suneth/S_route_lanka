SET @schema_name = DATABASE();

SET @has_brand = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'customer_cards'
      AND column_name = 'brand'
);

SET @has_card_brand = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'customer_cards'
      AND column_name = 'card_brand'
);

SET @sql = IF(
    @has_brand = 1 AND @has_card_brand = 0,
    'ALTER TABLE customer_cards CHANGE COLUMN brand card_brand VARCHAR(20) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_brand = 1 AND @has_card_brand = 1,
    'UPDATE customer_cards SET card_brand = COALESCE(NULLIF(card_brand, ''''), NULLIF(brand, ''''), ''VISA'')',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_brand = 1 AND @has_card_brand = 1,
    'ALTER TABLE customer_cards DROP COLUMN brand',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customer_cards
SET card_brand = 'VISA'
WHERE card_brand IS NULL OR card_brand = '';

ALTER TABLE customer_cards
    MODIFY COLUMN card_brand VARCHAR(20) NOT NULL;
