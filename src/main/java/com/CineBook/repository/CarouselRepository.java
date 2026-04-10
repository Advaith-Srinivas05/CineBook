package com.CineBook.repository;

import com.CineBook.model.CarouselImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CarouselRepository extends JpaRepository<CarouselImage, Long> {
	interface CarouselProjection {
		Long getId();
		String getImageName();
	}

	interface BannerListProjection {
		Long getId();
		String getImageName();
		Long getImageSizeBytes();
		Integer getImageWidth();
		Integer getImageHeight();
		String getAspectRatio();
		String getFileType();
	}

	interface BannerBinaryProjection {
		Long getId();
		byte[] getImageData();
		String getFileType();
		String getImageName();
	}

	@Query("SELECT c.id AS id, c.imageName AS imageName FROM CarouselImage c")
	List<CarouselProjection> findAllProjectedBy();

	@Query("""
			SELECT c.id AS id,
			       c.imageName AS imageName,
			       c.imageSizeBytes AS imageSizeBytes,
			       c.imageWidth AS imageWidth,
			       c.imageHeight AS imageHeight,
			       c.aspectRatio AS aspectRatio,
			       c.fileType AS fileType
			FROM CarouselImage c
			ORDER BY c.id
			""")
	List<BannerListProjection> findAllBannerListData();

	@Query("""
			SELECT c.id AS id,
			       c.imageData AS imageData,
			       c.fileType AS fileType,
			       c.imageName AS imageName
			FROM CarouselImage c
			WHERE c.id = :id
			""")
	Optional<BannerBinaryProjection> findBannerBinaryById(Long id);
}