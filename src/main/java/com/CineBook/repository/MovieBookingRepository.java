package com.CineBook.repository;

import com.CineBook.model.MovieBooking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieBookingRepository extends JpaRepository<MovieBooking, Long> {

        Optional<MovieBooking> findByPublicId(String publicId);

    @Query(value = "SELECT TRIM(seat_value) AS seat_number " +
            "FROM movie_bookings mb " +
            "CROSS JOIN LATERAL UNNEST(string_to_array(COALESCE(mb.seat_numbers, ''), ',')) AS seat_value " +
            "WHERE mb.show_id = :scheduleId " +
            "AND mb.show_date = :showDate " +
            "AND TRIM(seat_value) <> ''",
            nativeQuery = true)
    List<String> findBookedSeatNumbers(@Param("scheduleId") Long scheduleId,
                                       @Param("showDate") LocalDate showDate);

        @Query("SELECT mb FROM MovieBooking mb WHERE mb.id = :bookingId AND mb.user.id = :userId")
        Optional<MovieBooking> findOwnedBooking(@Param("bookingId") Long bookingId,
                                                                                        @Param("userId") Long userId);

        @Query("SELECT mb FROM MovieBooking mb " +
                        "JOIN FETCH mb.show s " +
                        "JOIN FETCH s.movie " +
                        "JOIN FETCH s.theater " +
                        "WHERE mb.user.id = :userId " +
                        "ORDER BY mb.createdAt DESC")
        List<MovieBooking> findHistoryByUserId(@Param("userId") Long userId);
}
