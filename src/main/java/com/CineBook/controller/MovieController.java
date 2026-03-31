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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
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
    public String indexString(@RequestParam(value = "city", required = false) String city,
                              Model model,
                              HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            return "redirect:/admin";
        }
        try {
            model.addAttribute("carouselImages", repository.findAllProjectedBy());

            java.util.List<String> cities = theaterRepository.findDistinctCities();
            model.addAttribute("cities", cities);

            String selectedCity = (city == null || city.isBlank()) ? null : city.trim();
            if ((selectedCity == null || selectedCity.isBlank()) && !cities.isEmpty()) {
                selectedCity = cities.get(0);
            }
            if (selectedCity != null) {
                for (String knownCity : cities) {
                    if (knownCity.equalsIgnoreCase(selectedCity)) {
                        selectedCity = knownCity;
                        break;
                    }
                }
            }

            model.addAttribute("selectedCity", selectedCity);
            if (selectedCity == null || selectedCity.isBlank()) {
                model.addAttribute("movies", java.util.Collections.emptyList());
            } else {
                model.addAttribute("movies", movieRepository.findDistinctMoviesByCity(selectedCity));
            }
        } catch (org.springframework.dao.DataAccessException ex) {
            // ex.printStackTrace();
            model.addAttribute("carouselImages", java.util.Collections.emptyList());
            model.addAttribute("cities", java.util.Collections.emptyList());
            model.addAttribute("selectedCity", null);
            model.addAttribute("movies", java.util.Collections.emptyList());
        }
        return "index";
    }

    @GetMapping("/movie/poster/{id}")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<byte[]> getMoviePoster(@org.springframework.web.bind.annotation.PathVariable Long id) {
        java.util.Optional<Movie> movieOpt = movieRepository.findById(id);
        if (movieOpt.isEmpty() || movieOpt.get().getPoster() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(movieOpt.get().getPoster());
    }

    @GetMapping("/movie")
    public String movie(@RequestParam("movieId") Long movieId,
                        @RequestParam(value = "city", required = false) String city,
                        @RequestParam(value = "date", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model,
                        HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";

        java.util.Optional<Movie> movieOpt = movieRepository.findById(movieId);
        if (movieOpt.isEmpty()) {
            return "redirect:/";
        }

        List<String> cities = theaterRepository.findDistinctCities();
        String selectedCity = (city == null || city.isBlank()) ? null : city.trim();
        if ((selectedCity == null || selectedCity.isBlank()) && !cities.isEmpty()) {
            selectedCity = cities.get(0);
        }
        if (selectedCity != null) {
            for (String knownCity : cities) {
                if (knownCity.equalsIgnoreCase(selectedCity)) {
                    selectedCity = knownCity;
                    break;
                }
            }
        }
        final String resolvedCity = selectedCity;

        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        List<LocalDate> dateOptions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dateOptions.add(LocalDate.now().plusDays(i));
        }

        List<ShowSchedule> schedules = showScheduleRepository.findByMovieId(movieId);
        List<ShowSchedule> filteredSchedules = schedules.stream()
                .filter(s -> s.getTheater() != null)
            .filter(s -> resolvedCity == null || resolvedCity.isBlank()
                || (s.getTheater().getCity() != null && s.getTheater().getCity().equalsIgnoreCase(resolvedCity)))
                .filter(s -> s.getStartDate() != null && s.getEndDate() != null && s.getStartTime() != null)
                .filter(s -> !selectedDate.isBefore(s.getStartDate()) && !selectedDate.isAfter(s.getEndDate()))
                .sorted(Comparator
                        .comparing((ShowSchedule s) -> s.getTheater().getCity(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(s -> s.getTheater().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ShowSchedule::getStartTime))
                .collect(Collectors.toList());

        LinkedHashMap<Long, TheaterShowView> groupedShows = new LinkedHashMap<>();
        for (ShowSchedule schedule : filteredSchedules) {
            Long theaterId = schedule.getTheater().getId();
            TheaterShowView theaterShow = groupedShows.computeIfAbsent(
                    theaterId,
                    id -> new TheaterShowView(
                            id,
                            schedule.getTheater().getName(),
                            schedule.getTheater().getCity(),
                            schedule.getTheater().getLocation()
                    )
            );
            theaterShow.getShows().add(new ShowTimeView(
                    schedule.getId(),
                    schedule.getStartTime(),
                    schedule.getScreen()
            ));
        }

        model.addAttribute("movie", movieOpt.get());
        model.addAttribute("cities", cities);
        model.addAttribute("selectedCity", selectedCity);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dateOptions", dateOptions);
        model.addAttribute("theaterShows", new ArrayList<>(groupedShows.values()));
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
        return "redirect:/?auth=login";
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
        String returnTo = UriUtils.encode("/admin", StandardCharsets.UTF_8);
        return "redirect:/?auth=login&returnTo=" + returnTo; // not logged in -> auth modal
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<String> addMovie(@RequestParam("title") String title,
                                           @RequestParam("duration") Integer duration,
                                           @RequestParam("language") String language,
                                           @RequestParam(value = "certification", required = false) String certification,
                                           @RequestParam(value = "description", required = false) String description,
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
            m.setCertification(certification);
            m.setDescription(description);
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
            map.put("certification", m.getCertification());
            map.put("description", m.getDescription());

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
                                              @RequestParam(value = "certification", required = false) String certification,
                                              @RequestParam(value = "description", required = false) String description,
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
            m.setCertification(certification);
            m.setDescription(description);
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
                                             @RequestParam("city") String city,
                                             @RequestParam(value = "location", required = false) String location,
                                             @RequestParam(value = "screen_count", required = false) Integer screenCount,
                                             HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            name = name == null ? "" : name.trim();
            city = city == null ? "" : city.trim();
            location = location == null ? null : location.trim();

            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body("Theater name is required");
            }
            if (city.isEmpty()) {
                return ResponseEntity.badRequest().body("City is required");
            }

            // prevent duplicate theater names
            if (theaterRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(409).body("Theater with this name already exists");
            }
            com.CineBook.model.Theater t = new com.CineBook.model.Theater();
            t.setName(name);
            t.setCity(city);
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
        java.util.List<com.CineBook.model.Theater> list = theaterRepository.findTop10ByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrLocationContainingIgnoreCase(q, q, q);
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        for (com.CineBook.model.Theater t : list) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("city", t.getCity());
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

    public static class TheaterShowView {
        private final Long theaterId;
        private final String theaterName;
        private final String city;
        private final String location;
        private final List<ShowTimeView> shows = new ArrayList<>();

        public TheaterShowView(Long theaterId, String theaterName, String city, String location) {
            this.theaterId = theaterId;
            this.theaterName = theaterName;
            this.city = city;
            this.location = location;
        }

        public Long getTheaterId() {
            return theaterId;
        }

        public String getTheaterName() {
            return theaterName;
        }

        public String getCity() {
            return city;
        }

        public String getLocation() {
            return location;
        }

        public List<ShowTimeView> getShows() {
            return shows;
        }
    }

    public static class ShowTimeView {
        private final Long scheduleId;
        private final LocalTime startTime;
        private final Integer screen;

        public ShowTimeView(Long scheduleId, LocalTime startTime, Integer screen) {
            this.scheduleId = scheduleId;
            this.startTime = startTime;
            this.screen = screen;
        }

        public Long getScheduleId() {
            return scheduleId;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public Integer getScreen() {
            return screen;
        }
    }
}