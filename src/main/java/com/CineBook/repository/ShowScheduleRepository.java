package com.CineBook.repository;

import com.CineBook.model.ShowSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {

    List<ShowSchedule> findByMovieId(Long movieId);

    @Query(value = "SELECT ss.id, m.title AS movie_title, ss.start_time, ss.start_date, ss.end_date, ss.screen " +
            "FROM show_schedules ss " +
            "JOIN movies m ON m.id = ss.movie_id " +
            "WHERE ss.theater_id = :theaterId " +
            "AND ss.movie_id = :movieId " +
            "ORDER BY ss.start_date, ss.start_time",
            nativeQuery = true)
    List<Object[]> findScheduledShowsByTheaterAndMovie(@Param("theaterId") Long theaterId,
                                                        @Param("movieId") Long movieId);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
            "FROM show_schedules ss " +
            "JOIN movies m ON m.id = ss.movie_id " +
            "WHERE ss.theater_id = :theaterId " +
            "AND ss.screen = :screen " +
            "AND ss.end_date >= :startDate " +
            "AND ss.start_date <= :endDate " +
            "AND (timestamp '2000-01-01' + CAST(:startTime AS time)) < " +
            "    (timestamp '2000-01-01' + ss.start_time + (COALESCE(m.duration_minutes, 0) * interval '1 minute')) " +
            "AND (timestamp '2000-01-01' + ss.start_time) < " +
            "    (timestamp '2000-01-01' + CAST(:startTime AS time) + (:durationMinutes * interval '1 minute'))",
            nativeQuery = true)
    boolean existsOverlappingSlot(@Param("theaterId") Long theaterId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("screen") Integer screen,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("durationMinutes") Integer durationMinutes);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
            "FROM show_schedules ss " +
            "JOIN movies m ON m.id = ss.movie_id " +
            "WHERE ss.theater_id = :theaterId " +
            "AND ss.screen = :screen " +
            "AND ss.end_date >= :startDate " +
            "AND ss.start_date <= :endDate " +
            "AND ss.id <> :excludedScheduleId " +
            "AND (timestamp '2000-01-01' + CAST(:startTime AS time)) < " +
            "    (timestamp '2000-01-01' + ss.start_time + (COALESCE(m.duration_minutes, 0) * interval '1 minute')) " +
            "AND (timestamp '2000-01-01' + ss.start_time) < " +
            "    (timestamp '2000-01-01' + CAST(:startTime AS time) + (:durationMinutes * interval '1 minute'))",
            nativeQuery = true)
    boolean existsOverlappingSlotExcludingSlot(@Param("theaterId") Long theaterId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("screen") Integer screen,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("durationMinutes") Integer durationMinutes,
                                               @Param("excludedScheduleId") Long excludedScheduleId);
}
