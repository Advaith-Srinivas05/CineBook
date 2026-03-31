package com.CineBook.repository;

import com.CineBook.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

	Optional<Movie> findByTitle(String title);

	java.util.List<Movie> findTop10ByTitleContainingIgnoreCase(String title);

	@Query(value = "SELECT DISTINCT m.* " +
			"FROM movies m " +
			"JOIN show_schedules ss ON ss.movie_id = m.id " +
			"JOIN theaters t ON t.id = ss.theater_id " +
			"WHERE LOWER(t.city) = LOWER(:city) " +
			"ORDER BY m.title",
			nativeQuery = true)
	java.util.List<Movie> findDistinctMoviesByCity(@Param("city") String city);


}
