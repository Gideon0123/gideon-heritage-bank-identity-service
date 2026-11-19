ALTER TABLE branches
ALTER COLUMN country_code TYPE VARCHAR(2)
USING TRIM(country_code)::VARCHAR(2);