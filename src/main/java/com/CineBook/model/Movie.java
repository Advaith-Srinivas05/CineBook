package com.CineBook.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "movies", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(columnNames = {"title"})})
public class Movie {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	private String language;

	private Double rating = 0.0;

	@Column(name = "poster", columnDefinition = "bytea")
	private byte[] poster;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) createdAt = OffsetDateTime.now();
	}

	// getters and setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public Integer getDurationMinutes() { return durationMinutes; }
	public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

	public String getLanguage() { return language; }
	public void setLanguage(String language) { this.language = language; }

	public Double getRating() { return rating; }
	public void setRating(Double rating) { this.rating = rating; }

	public byte[] getPoster() { return poster; }
	public void setPoster(byte[] poster) { this.poster = poster; }

	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
