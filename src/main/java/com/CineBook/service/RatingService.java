package com.CineBook.service;

import com.CineBook.model.Movie;
import com.CineBook.model.MovieRating;
import com.CineBook.model.User;
import com.CineBook.repository.MovieRatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {

    @Autowired
    private MovieRatingRepository movieRatingRepository;

    @Transactional
    public MovieRating submitOrUpdateRating(User user, Movie movie, int ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Optional<MovieRating> existing = movieRatingRepository.findByMovieIdAndUserId(movie.getId(), user.getId());
        MovieRating rating = existing.orElseGet(MovieRating::new);
        rating.setMovie(movie);
        rating.setUser(user);
        rating.setRating(ratingValue);
        return movieRatingRepository.save(rating);
    }

    @Transactional(readOnly = true)
    public List<MovieRating> getRatingsForMovie(Long movieId) {
        return movieRatingRepository.findByMovieId(movieId);
    }

    @Transactional(readOnly = true)
    public Optional<MovieRating> getUserRating(Long movieId, Long userId) {
        return movieRatingRepository.findByMovieIdAndUserId(movieId, userId);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Long movieId) {
        Double avg = movieRatingRepository.findAverageRatingByMovieId(movieId);
        return avg == null ? 0.0 : avg;
    }

    @Transactional(readOnly = true)
    public long getTotalRatings(Long movieId) {
        return movieRatingRepository.countByMovieId(movieId);
    }
}
