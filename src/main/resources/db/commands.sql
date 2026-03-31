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

-- Movie ratings migration (run once on existing DB if needed)
ALTER TABLE movie_ratings ALTER COLUMN user_id TYPE BIGINT;
UPDATE movie_ratings
SET rating = LEAST(GREATEST(rating, 1), 5);

ALTER TABLE movie_ratings DROP CONSTRAINT IF EXISTS movie_ratings_rating_check;
ALTER TABLE movie_ratings
    ADD CONSTRAINT movie_ratings_rating_check CHECK (rating >= 1 AND rating <= 5);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_movie_ratings_user'
    ) THEN
        ALTER TABLE movie_ratings
            ADD CONSTRAINT fk_movie_ratings_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_movie_ratings_movie'
    ) THEN
        ALTER TABLE movie_ratings
            ADD CONSTRAINT fk_movie_ratings_movie
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE;
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_movie_ratings_movie_user ON movie_ratings(movie_id, user_id);
CREATE INDEX IF NOT EXISTS idx_movie_ratings_movie ON movie_ratings(movie_id);