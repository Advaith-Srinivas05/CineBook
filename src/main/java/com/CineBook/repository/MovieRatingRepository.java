package com.CineBook.repository;

import com.CineBook.model.MovieRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRatingRepository extends JpaRepository<MovieRating, Long> {

    Optional<MovieRating> findByMovieIdAndUserId(Long movieId, Long userId);

    List<MovieRating> findByMovieId(Long movieId);

    long countByMovieId(Long movieId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM MovieRating r WHERE r.movie.id = :movieId")
    Double findAverageRatingByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT mr FROM MovieRating mr " +
            "JOIN FETCH mr.movie " +
            "WHERE mr.user.id = :userId " +
            "ORDER BY mr.createdAt DESC")
    List<MovieRating> findHistoryByUserId(@Param("userId") Long userId);
}
