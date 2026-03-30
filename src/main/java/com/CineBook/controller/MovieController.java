package com.CineBook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.model.Movie;
import com.CineBook.model.ShowSchedule;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DataIntegrityViolationException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import jakarta.transaction.Transactional;

@Controller
public class MovieController {
    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private com.CineBook.repository.TheaterRepository theaterRepository;
    
    @Autowired
    private com.CineBook.repository.ShowScheduleRepository showScheduleRepository;

    @GetMapping("/")
    public String indexString(Model model, HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            return "redirect:/admin";
        }
        try {
            model.addAttribute("carouselImages", repository.findAllProjectedBy());
        } catch (org.springframework.dao.DataAccessException ex) {
            // ex.printStackTrace();
            model.addAttribute("carouselImages", java.util.Collections.emptyList());
        }
        return "index";
    }
    @GetMapping("/movie")
    public String movie(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "movie";
    }

    @GetMapping("/tickets")
    public String tickets(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "tickets";
    }

    @GetMapping("/seats")
    public String seats(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "seats";
    }

    @GetMapping("/payment")
    public String payment(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "payment";
    }

    @GetMapping("/success")
    public String success(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "success";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        if (username != null) return "redirect:/";
        return "login";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            return "admin";
        }
        if (username != null) {
            return "redirect:/"; // logged-in non-admin users -> index
        }
        return "redirect:/login"; // not logged in -> login
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<String> addMovie(@RequestParam("title") String title,
                                           @RequestParam("duration") Integer duration,
                                           @RequestParam("language") String language,
                                           @RequestParam("poster") MultipartFile poster,
                                           HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            // prevent duplicate movie titles
            if (movieRepository.findByTitle(title).isPresent()) {
                return ResponseEntity.status(409).body("Movie with this title already exists");
            }
            Movie m = new Movie();
            m.setTitle(title);
            m.setDurationMinutes(duration);
            m.setLanguage(language);
            if (poster != null && !poster.isEmpty()) {
                m.setPoster(poster.getBytes());
            }
            movieRepository.save(m);
            return ResponseEntity.ok("OK");
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to read poster");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @GetMapping("/api/admin/movies")
    public ResponseEntity<java.util.List<java.util.Map<String,Object>>> listAllMovies(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).build();
        }
        java.util.List<Movie> list = movieRepository.findAll();
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        for (Movie m : list) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("duration", m.getDurationMinutes());
            map.put("language", m.getLanguage());

            String status = "Upcoming";
            try {
                List<ShowSchedule> schedules = showScheduleRepository.findByMovieId(m.getId());
                if (schedules != null && !schedules.isEmpty()) {
                    boolean anyOngoing = false;
                    boolean anyFuture = false;
                    Integer movieDuration = m.getDurationMinutes();
                    int duration = movieDuration == null ? 0 : movieDuration;
                    for (ShowSchedule s : schedules) {
                        if (s.getStartDate() == null || s.getEndDate() == null || s.getStartTime() == null) {
                            continue;
                        }
                        if (nowDate.isBefore(s.getStartDate())) {
                            anyFuture = true;
                        }
                        if (!nowDate.isBefore(s.getStartDate()) && !nowDate.isAfter(s.getEndDate())) {
                            LocalDateTime start = LocalDateTime.of(nowDate, s.getStartTime());
                            LocalDateTime end = start.plusMinutes(duration);
                            LocalDateTime nowDateTime = LocalDateTime.of(nowDate, nowTime);
                            if (!nowDateTime.isBefore(start) && nowDateTime.isBefore(end)) {
                                anyOngoing = true;
                                break;
                            }
                            if (nowDateTime.isBefore(start)) {
                                anyFuture = true;
                            }
                            if (anyOngoing) {
                                anyOngoing = true;
                                break;
                            }
                            if (nowDate.isBefore(s.getEndDate())) {
                                anyFuture = true;
                            }
                        }
                    }
                    if (anyOngoing) status = "Ongoing";
                    else if (anyFuture) status = "Upcoming";
                    else status = "Archived";
                }
            } catch (Exception ignored) {
                status = "Upcoming";
            }
            map.put("status", status);
            out.add(map);
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping("/admin/movies/{id}/update")
    public ResponseEntity<String> updateMovie(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                                              @RequestParam("title") String title,
                                              @RequestParam("duration") Integer duration,
                                              @RequestParam("language") String language,
                                              @RequestParam(value = "poster", required = false) MultipartFile poster,
                                              HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            java.util.Optional<Movie> mOpt = movieRepository.findById(id);
            if (mOpt.isEmpty()) return ResponseEntity.status(404).body("Not found");
            Movie m = mOpt.get();
            m.setTitle(title);
            m.setDurationMinutes(duration);
            m.setLanguage(language);
            if (poster != null && !poster.isEmpty()) {
                try {
                    m.setPoster(poster.getBytes());
                } catch (IOException ex) {
                    return ResponseEntity.status(500).body("Failed to read poster");
                }
            }
            movieRepository.save(m);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @PostMapping("/admin/movies/{id}/delete")
    public ResponseEntity<String> deleteMovie(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                                             HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            if (!movieRepository.existsById(id)) return ResponseEntity.status(404).body("Not found");
            movieRepository.deleteById(id);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @PostMapping("/admin/theaters")
    public ResponseEntity<String> addTheater(@RequestParam("name") String name,
                                             @RequestParam(value = "location", required = false) String location,
                                             @RequestParam(value = "screen_count", required = false) Integer screenCount,
                                             HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            // prevent duplicate theater names
            if (theaterRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(409).body("Theater with this name already exists");
            }
            com.CineBook.model.Theater t = new com.CineBook.model.Theater();
            t.setName(name);
            t.setLocation(location);
            t.setScreenCount(screenCount == null ? 1 : screenCount);
            theaterRepository.save(t);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @GetMapping("/api/movies/search")
    public ResponseEntity<java.util.List<java.util.Map<String,Object>>> searchMovies(@RequestParam(value = "q", required = false) String q) {
        if (q == null) q = "";
        java.util.List<Movie> list = movieRepository.findTop10ByTitleContainingIgnoreCase(q);
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        for (Movie m : list) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            out.add(map);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/api/theaters/search")
    public ResponseEntity<java.util.List<java.util.Map<String,Object>>> searchTheaters(@RequestParam(value = "q", required = false) String q) {
        if (q == null) q = "";
        java.util.List<com.CineBook.model.Theater> list = theaterRepository.findTop10ByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(q, q);
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        for (com.CineBook.model.Theater t : list) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("location", t.getLocation());
            map.put("screenCount", t.getScreenCount());
            out.add(map);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/api/admin/shows")
    public ResponseEntity<List<Map<String, Object>>> getShows(@RequestParam("theater_id") Long theaterId,
                                                              @RequestParam("movie_id") Long movieId) {
        List<Object[]> results = showScheduleRepository.findScheduledShowsByTheaterAndMovie(theaterId, movieId);

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("movieTitle", row[1]);
            map.put("startTime", row[2]);
            map.put("startDate", row[3]);
            map.put("endDate", row[4]);
            map.put("screen", row[5]);
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/admin/shows/{id}/update", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<String> updateScheduledSlot(@org.springframework.web.bind.annotation.PathVariable("id") Long scheduleId,
                                                      @RequestParam("start_date") String startDateStr,
                                                      @RequestParam("end_date") String endDateStr,
                                                      @RequestParam("screen") Integer screen,
                                                      HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        java.util.Optional<ShowSchedule> scheduleOpt = showScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Show schedule not found");
        }

        ShowSchedule schedule = scheduleOpt.get();
        Movie movie = schedule.getMovie();
        com.CineBook.model.Theater theater = schedule.getTheater();

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid start or end date");
        }

        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body("End date must be on or after start date");
        }

        if (screen == null || screen < 1) {
            return ResponseEntity.badRequest().body("Invalid screen number");
        }
        if (theater.getScreenCount() != null && screen > theater.getScreenCount()) {
            return ResponseEntity.badRequest().body("Screen number exceeds theater screen count");
        }

        Integer durationMinutes = movie.getDurationMinutes();
        if (durationMinutes == null || durationMinutes <= 0) {
            return ResponseEntity.badRequest().body("Movie duration must be greater than zero");
        }

        boolean overlaps = showScheduleRepository.existsOverlappingSlotExcludingSlot(
                theater.getId(),
                startDate,
                endDate,
                screen,
                schedule.getStartTime(),
                durationMinutes,
                schedule.getId()
        );
        if (overlaps) {
            return ResponseEntity.status(409).body("Timeslot overlaps with an existing schedule on the same screen");
        }

        schedule.setScreen(screen);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        showScheduleRepository.save(schedule);

        return ResponseEntity.ok("OK");
    }

    @PostMapping(value = "/admin/shows", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<String> scheduleShow(@RequestParam("movie_id") Long movieId,
                                               @RequestParam("theater_id") Long theaterId,
                                               @RequestParam("start_date") String startDateStr,
                                               @RequestParam("end_date") String endDateStr,
                                               @RequestParam("timeslot_time") List<String> timeslotTimes,
                                               @RequestParam("timeslot_screen") List<Integer> timeslotScreens,
                                               HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            java.util.Optional<Movie> mOpt = movieRepository.findById(movieId);
            java.util.Optional<com.CineBook.model.Theater> tOpt = theaterRepository.findById(theaterId);
            if (mOpt.isEmpty() || tOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid movie or theater");
            }

            Movie movie = mOpt.get();
            com.CineBook.model.Theater theater = tOpt.get();

            LocalDate startDate;
            LocalDate endDate;
            try {
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("Invalid start or end date");
            }

            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest().body("End date must be on or after start date");
            }

            if (timeslotTimes == null || timeslotScreens == null || timeslotTimes.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one timeslot is required");
            }

            if (timeslotTimes.size() != timeslotScreens.size()) {
                return ResponseEntity.badRequest().body("Timeslot inputs are invalid");
            }

            Integer durationMinutes = movie.getDurationMinutes();
            if (durationMinutes == null || durationMinutes <= 0) {
                return ResponseEntity.badRequest().body("Movie duration must be greater than zero");
            }

            List<LocalTime> parsedTimes = new ArrayList<>();
            List<Integer> parsedScreens = new ArrayList<>();

            for (int i = 0; i < timeslotTimes.size(); i++) {
                String timeStr = timeslotTimes.get(i);
                Integer screen = timeslotScreens.get(i);

                if (timeStr == null || timeStr.isBlank()) {
                    return ResponseEntity.badRequest().body("Each timeslot must have a start time");
                }
                LocalTime slotStart;
                try {
                    slotStart = LocalTime.parse(timeStr);
                } catch (Exception ex) {
                    return ResponseEntity.badRequest().body("Invalid timeslot time format");
                }

                if (screen == null || screen < 1) {
                    return ResponseEntity.badRequest().body("Invalid screen number in timeslots");
                }
                if (theater.getScreenCount() != null && screen > theater.getScreenCount()) {
                    return ResponseEntity.badRequest().body("Screen number exceeds theater screen count");
                }

                parsedTimes.add(slotStart);
                parsedScreens.add(screen);
            }

            for (int i = 0; i < parsedTimes.size(); i++) {
                for (int j = i + 1; j < parsedTimes.size(); j++) {
                    if (!parsedScreens.get(i).equals(parsedScreens.get(j))) {
                        continue;
                    }
                    if (timesOverlap(parsedTimes.get(i), durationMinutes, parsedTimes.get(j), durationMinutes)) {
                        return ResponseEntity.status(409).body("Timeslots overlap on the same screen");
                    }
                }
            }

            for (int i = 0; i < parsedTimes.size(); i++) {
                boolean overlap = showScheduleRepository.existsOverlappingSlot(
                        theaterId,
                        startDate,
                        endDate,
                        parsedScreens.get(i),
                        parsedTimes.get(i),
                        durationMinutes
                );
                if (overlap) {
                    return ResponseEntity.status(409).body("Timeslot overlaps with an existing schedule on the same screen");
                }
            }

            List<ShowSchedule> schedulesToSave = new ArrayList<>();
            for (int i = 0; i < parsedTimes.size(); i++) {
                ShowSchedule schedule = new ShowSchedule();
                schedule.setMovie(movie);
                schedule.setTheater(theater);
                schedule.setStartDate(startDate);
                schedule.setEndDate(endDate);
                schedule.setStartTime(parsedTimes.get(i));
                schedule.setScreen(parsedScreens.get(i));
                schedulesToSave.add(schedule);
            }

            showScheduleRepository.saveAll(schedulesToSave);
            return ResponseEntity.ok("OK");
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body("Timeslot overlaps with an existing schedule on the same screen");
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = "Unexpected server error";
            }
            return ResponseEntity.status(500).body("Failed to schedule shows: " + message);
        }
    }

    @PostMapping("/admin/shows/{id}/delete")
    @Transactional
    public ResponseEntity<String> deleteScheduledSlot(@org.springframework.web.bind.annotation.PathVariable("id") Long scheduleId,
                                                      HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        java.util.Optional<ShowSchedule> scheduleOpt = showScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }

        showScheduleRepository.delete(scheduleOpt.get());
        return ResponseEntity.ok("OK");
    }

    private boolean timesOverlap(LocalTime firstStart,
                                 int firstDurationMinutes,
                                 LocalTime secondStart,
                                 int secondDurationMinutes) {
        LocalDate baseDate = LocalDate.of(2000, 1, 1);
        LocalDateTime firstStartDateTime = LocalDateTime.of(baseDate, firstStart);
        LocalDateTime firstEndDateTime = firstStartDateTime.plusMinutes(firstDurationMinutes);
        LocalDateTime secondStartDateTime = LocalDateTime.of(baseDate, secondStart);
        LocalDateTime secondEndDateTime = secondStartDateTime.plusMinutes(secondDurationMinutes);

        return firstStartDateTime.isBefore(secondEndDateTime)
                && secondStartDateTime.isBefore(firstEndDateTime);
    }
}