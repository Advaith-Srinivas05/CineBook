package com.CineBook.repository;

import com.CineBook.model.CarouselImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CarouselRepository extends JpaRepository<CarouselImage, Long> {
	interface CarouselProjection {
		Long getId();
		String getImageName();
	}

	@Query("SELECT c.id AS id, c.imageName AS imageName FROM CarouselImage c")
	List<CarouselProjection> findAllProjectedBy();
}