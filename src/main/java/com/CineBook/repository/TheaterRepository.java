package com.CineBook.repository;

import com.CineBook.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

	Optional<Theater> findByName(String name);

	java.util.List<Theater> findTop10ByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(String name, String location);


}
