package com.CineBook.repository;

import com.CineBook.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

	Optional<Theater> findByName(String name);

	java.util.List<Theater> findTop10ByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrLocationContainingIgnoreCase(String name, String city, String location);

	@Query("SELECT DISTINCT t.city FROM Theater t ORDER BY t.city")
	java.util.List<String> findDistinctCities();


}
