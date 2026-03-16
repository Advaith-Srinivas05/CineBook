package com.CineBook.repository;

import com.CineBook.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {

	java.util.List<Show> findByMovieId(Long movieId);

}
