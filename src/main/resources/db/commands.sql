DO $$
DECLARE
    file TEXT;
    folder TEXT := 'D:\Advaith\clg_stuff\3rd year\sem 6\OOAD\proj\BookMyShow-Clone\img\banner\';
BEGIN
    FOR file IN SELECT pg_ls_dir(folder)
    LOOP
        INSERT INTO carousel_images(image_name, image_data)
        VALUES (
            file,
            pg_read_binary_file(folder || file)
        );
    END LOOP;
END
$$;

SELECT id, image_name, length(image_data)
FROM carousel_images;

INSERT INTO users (username, email, password_hash) VALUES ('Admin','admin@example.com','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918');

-- Theater city/location split migration (run once on existing DB if needed)
ALTER TABLE theaters ADD COLUMN IF NOT EXISTS city TEXT;

UPDATE theaters
SET city = NULLIF(trim(split_part(location, ',', 1)), '')
WHERE city IS NULL;

UPDATE theaters
SET location = NULLIF(trim(substring(location from position(',' in location) + 1)), '')
WHERE location IS NOT NULL AND position(',' in location) > 0;

ALTER TABLE theaters ALTER COLUMN city SET NOT NULL;