package com.CineBook.controller;

import com.CineBook.model.Movie;
import com.CineBook.model.MovieRating;
import com.CineBook.model.User;
import com.CineBook.repository.MovieRepository;
import com.CineBook.repository.UserRepository;
import com.CineBook.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/ratings")
    public ResponseEntity<?> submitRating(@RequestBody RatingRequest request, HttpSession session) {
        if (request == null || request.movieId == null || request.rating == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "movieId and rating are required"));
        }

        if (request.rating < 1 || request.rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "rating must be between 1 and 5"));
        }

        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please login to submit a rating"));
        }

        Optional<Movie> movieOpt = movieRepository.findById(request.movieId);
        if (movieOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Movie not found"));
        }

        MovieRating saved = ratingService.submitOrUpdateRating(userOpt.get(), movieOpt.get(), request.rating);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("movieId", request.movieId);
        response.put("userId", userOpt.get().getId());
        response.put("rating", saved.getRating());
        response.put("averageRating", ratingService.getAverageRating(request.movieId));
        response.put("totalRatings", ratingService.getTotalRatings(request.movieId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ratings/{movieId}")
    public ResponseEntity<?> getMovieRatings(@PathVariable("movieId") Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Movie not found"));
        }

        List<MovieRating> ratings = ratingService.getRatingsForMovie(movieId);
        List<Map<String, Object>> ratingRows = new ArrayList<>();
        for (MovieRating rating : ratings) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", rating.getUser().getId());
            row.put("rating", rating.getRating());
            OffsetDateTime createdAt = rating.getCreatedAt();
            row.put("createdAt", createdAt == null ? null : createdAt.toString());
            ratingRows.add(row);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("movieId", movieId);
        response.put("averageRating", ratingService.getAverageRating(movieId));
        response.put("totalRatings", ratingService.getTotalRatings(movieId));
        response.put("ratings", ratingRows);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ratings/user/{movieId}")
    public ResponseEntity<?> getCurrentUserRating(@PathVariable("movieId") Long movieId, HttpSession session) {
        if (!movieRepository.existsById(movieId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Movie not found"));
        }

        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("authenticated", false);
            response.put("movieId", movieId);
            response.put("rating", null);
            return ResponseEntity.ok(response);
        }

        Optional<MovieRating> ratingOpt = ratingService.getUserRating(movieId, userOpt.get().getId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", true);
        response.put("movieId", movieId);
        response.put("userId", userOpt.get().getId());
        response.put("rating", ratingOpt.map(MovieRating::getRating).orElse(null));
        return ResponseEntity.ok(response);
    }

    private Optional<User> getAuthenticatedUser(HttpSession session) {
        Object usernameAttr = session.getAttribute("username");
        if (!(usernameAttr instanceof String username) || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    public static class RatingRequest {
        public Long movieId;
        public Integer rating;
    }
}
