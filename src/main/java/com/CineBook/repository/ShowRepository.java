package com.CineBook.repository;

import com.CineBook.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

	List<Show> findByMovieId(Long movieId);

	    @Query(value = "SELECT s.id, m.title AS movieTitle, t.name AS theaterName, s.screen, s.start_time, s.end_time " +
		    "FROM shows s " +
		    "JOIN movies m ON s.movie_id = m.id " +
		    "JOIN theaters t ON s.theater_id = t.id " +
			"WHERE s.theater_id = :theaterId " +
			"AND DATE(s.start_time) = CAST(:date AS date) " +
		    "ORDER BY s.start_time",
		    nativeQuery = true)
	    List<Object[]> findShowsByTheaterAndDate(@Param("theaterId") Long theaterId,
						     @Param("date") String date);

}
