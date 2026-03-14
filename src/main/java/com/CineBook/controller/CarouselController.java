package com.CineBook.controller;

import com.CineBook.model.CarouselImage;
import com.CineBook.repository.CarouselRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CarouselController {

    @Autowired
    private CarouselRepository repository;

    @GetMapping("/carousel/image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        CarouselImage image = repository.findById(id).orElse(null);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(image.getImageData());
    }
}