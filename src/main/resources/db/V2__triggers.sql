-- Validate that schedules do not overlap on the same theater and screen

CREATE OR REPLACE FUNCTION check_schedule_overlap()
RETURNS trigger AS $$
DECLARE
  v_new_duration INTEGER;
BEGIN
  SELECT COALESCE(m.duration_minutes, 0)
  INTO v_new_duration
  FROM movies m
  WHERE m.id = NEW.movie_id;

  IF v_new_duration IS NULL THEN
    RAISE EXCEPTION 'Invalid movie_id %', NEW.movie_id;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM show_schedules es
    JOIN movies em ON em.id = es.movie_id
    WHERE es.theater_id = NEW.theater_id
      AND es.screen = NEW.screen
      AND es.end_date >= NEW.start_date
      AND es.start_date <= NEW.end_date
      AND (timestamp '2000-01-01' + NEW.start_time)
            < (timestamp '2000-01-01' + es.start_time + (COALESCE(em.duration_minutes, 0) * interval '1 minute'))
      AND (timestamp '2000-01-01' + es.start_time)
            < (timestamp '2000-01-01' + NEW.start_time + (v_new_duration * interval '1 minute'))
      AND (TG_OP <> 'UPDATE' OR es.id <> NEW.id)
  ) THEN
    RAISE EXCEPTION 'Schedule overlaps with an existing schedule on the same theater and screen';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_schedule_overlap ON show_schedules;

CREATE TRIGGER trg_check_schedule_overlap
BEFORE INSERT OR UPDATE ON show_schedules
FOR EACH ROW
EXECUTE FUNCTION check_schedule_overlap();
