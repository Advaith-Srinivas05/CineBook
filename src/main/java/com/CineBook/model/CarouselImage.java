package com.CineBook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "carousel_images")
public class CarouselImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imageName;

    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    public Long getId() {
        return id;
    }

    public String getImageName() {
        return imageName;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
}