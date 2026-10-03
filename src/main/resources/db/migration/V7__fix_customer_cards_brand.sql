UPDATE customer_cards
SET brand = 'VISA'
WHERE brand IS NULL OR brand = '';

ALTER TABLE customer_cards
    MODIFY COLUMN brand VARCHAR(20) NOT NULL;
