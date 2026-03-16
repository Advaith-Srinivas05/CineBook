-- PostgreSQL triggers to auto-calculate show end_time from movie duration

-- Function to set NEW.end_time before insert or update on shows
CREATE OR REPLACE FUNCTION set_show_end_time()
RETURNS trigger AS $$
BEGIN
  NEW.end_time := NEW.start_time + ((SELECT COALESCE(duration_minutes,0) FROM movies WHERE id = NEW.movie_id) * interval '1 minute');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_show_end_time
BEFORE INSERT OR UPDATE ON shows
FOR EACH ROW
EXECUTE FUNCTION set_show_end_time();

-- Function to update existing shows when a movie's duration changes
CREATE OR REPLACE FUNCTION update_shows_on_movie_duration_change()
RETURNS trigger AS $$
BEGIN
  -- update end_time for all shows of this movie
  UPDATE shows
  SET end_time = start_time + (NEW.duration_minutes * interval '1 minute')
  WHERE movie_id = NEW.id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_shows_on_movie_update
AFTER UPDATE OF duration_minutes ON movies
FOR EACH ROW
EXECUTE FUNCTION update_shows_on_movie_duration_change();
