CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) UNIQUE,
  password_hash CHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE login_attempts (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100),
  user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  success BOOLEAN NOT NULL,
  attempted_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_login_attempts_username ON login_attempts(username);

CREATE TABLE carousel_images (
    id SERIAL PRIMARY KEY,
    image_name VARCHAR(255),
    image_data BYTEA NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE movies (
  id SERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  duration_minutes INTEGER NOT NULL,
  language VARCHAR(100),
  certification VARCHAR(20),
  description TEXT,
  rating NUMERIC(3,2) NOT NULL DEFAULT 0 CHECK (rating >= 0 AND rating <= 10),
  poster BYTEA,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE theaters (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  city TEXT NOT NULL,
  location TEXT,
  screen_count INTEGER NOT NULL DEFAULT 1,
  price INTEGER NOT NULL DEFAULT 250 CHECK (price > 0),
  elite_price INTEGER NOT NULL DEFAULT 350 CHECK (elite_price > 0)
);

CREATE TABLE show_schedules (
  id SERIAL PRIMARY KEY,
  movie_id INTEGER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
  theater_id INTEGER NOT NULL REFERENCES theaters(id) ON DELETE CASCADE,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  start_time TIME NOT NULL,
  screen INTEGER NOT NULL CHECK (screen >= 1),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (end_date >= start_date)
);

CREATE INDEX idx_show_schedules_theater_screen_date_time
  ON show_schedules(theater_id, screen, start_date, end_date, start_time);

CREATE INDEX idx_show_schedules_movie
  ON show_schedules(movie_id);

CREATE TABLE movie_bookings (
  id BIGSERIAL PRIMARY KEY,
  show_id INTEGER NOT NULL REFERENCES show_schedules(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  show_date DATE NOT NULL,
  seat_count INTEGER NOT NULL CHECK (seat_count > 0),
  total_price INTEGER NOT NULL CHECK (total_price > 0),
  seat_numbers TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movie_bookings_show_date
  ON movie_bookings(show_id, show_date);

CREATE INDEX idx_movie_bookings_user
  ON movie_bookings(user_id);

CREATE TABLE movie_ratings (
  id SERIAL PRIMARY KEY,
  movie_id INTEGER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_movie_ratings_movie_user ON movie_ratings(movie_id, user_id);
CREATE INDEX idx_movie_ratings_movie ON movie_ratings(movie_id);