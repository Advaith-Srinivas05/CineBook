DO $$
DECLARE
    file TEXT;
    project_path TEXT := '${projectRootPath}';
    folder_path TEXT := 'src\\main\\resources\\static\\images\\banner\\';
    folder TEXT;
BEGIN
    folder := project_path || folder_path;

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

INSERT INTO users (username, email, password_hash)
VALUES ('Admin','admin@admin.com','8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918');

DO $$
DECLARE
    project_path TEXT := '${projectRootPath}';
    folder_path TEXT := 'src\\main\\resources\\static\\images\\posters\\';
    base_path TEXT;
BEGIN
    base_path := project_path || folder_path;

    INSERT INTO movies (title, duration_minutes, language, certification, description, rating, poster)
    VALUES
    ('Dhurandhar',150,'Hindi','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster1.png')),
    ('Project Hail Mary',140,'English','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster2.png')),
    ('Biker',130,'English','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster3.png')),
    ('The Super Mario Galaxy Movie',120,'English','U','Placeholder description',0,pg_read_binary_file(base_path || 'poster4.png')),
    ('Hoppers',125,'English','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster5.png')),
    ('The Drama',135,'English','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster6.png')),
    ('Prathichaya',145,'Malayalam','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster7.png')),
    ('Ready or Not 2',110,'English','A','Placeholder description',0,pg_read_binary_file(base_path || 'poster8.png')),
    ('Wuthering Heights',150,'English','U','Placeholder description',0,pg_read_binary_file(base_path || 'poster9.png')),
    ('Youth',140,'English','UA','Placeholder description',0,pg_read_binary_file(base_path || 'poster10.png'));
END
$$;

INSERT INTO theaters (name, city, location, screen_count) VALUES
('PVR Nexus', 'Bengaluru', 'Koramangala', 8),
('INOX Mantri Square', 'Bengaluru', 'Malleshwaram', 7),
('Cinepolis Forum', 'Hyderabad', 'Kukatpally', 6),
('AGS Cinemas', 'Chennai', 'T. Nagar', 5),
('Lulu PVR', 'Kochi', 'Edappally', 9),
('SPI Escape', 'Chennai', 'Royapettah', 4),
('Miraj Cinemas', 'Mysuru', 'Jayalakshmipuram', 4),
('Carnival City Center', 'Mangaluru', 'Hampankatta', 4),
('Asian GPR Multiplex', 'Vijayawada', 'MG Road', 6),
('Sathyam Cinemas', 'Chennai', 'Royapettah', 5);

DO $$
DECLARE
    d INT;
    m RECORD;
    t RECORD;
    i INT;
    show_times TIME[] := ARRAY['10:00','13:30','17:00'];
    show_count INT;
    screen_no INT;
    slot_taken BOOLEAN;
BEGIN
    FOR d IN 0..13 LOOP
        FOR t IN SELECT * FROM theaters LOOP
            FOR m IN SELECT id FROM movies LOOP
                -- Create 1-3 shows per movie per theater.
                show_count := ((m.id + t.id) % 3) + 1;
                FOR i IN 1..show_count LOOP
                    -- Find a free screen at the selected slot.
                    FOR screen_no IN 1..t.screen_count LOOP
                        SELECT EXISTS (
                            SELECT 1 FROM show_schedules
                            WHERE theater_id = t.id
                            AND screen = screen_no
                            AND start_date = CURRENT_DATE + d
                            AND start_time = show_times[i]
                        ) INTO slot_taken;

                        IF NOT slot_taken THEN
                            INSERT INTO show_schedules (movie_id, theater_id, start_date, end_date, start_time, screen)
                            VALUES(m.id, t.id, CURRENT_DATE + d, CURRENT_DATE + d, show_times[i], screen_no);
                            EXIT;
                        END IF;
                    END LOOP;
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;
END
$$;
