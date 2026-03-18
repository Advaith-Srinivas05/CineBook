package com.CineBook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.model.Movie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class MovieController {
    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private com.CineBook.repository.TheaterRepository theaterRepository;
    
    @Autowired
    private com.CineBook.repository.ShowRepository showRepository;

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
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        for (Movie m : list) {
            java.util.Map<String,Object> map = new java.util.HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("duration", m.getDurationMinutes());
            map.put("language", m.getLanguage());

            java.util.List<com.CineBook.model.Show> shows = showRepository.findByMovieId(m.getId());
            String status;
            if (shows == null || shows.isEmpty()) {
                status = "Upcoming";
            } else {
                boolean anyOngoing = false;
                boolean anyFuture = false;
                for (com.CineBook.model.Show s : shows) {
                    if (s.getStartTime() != null && s.getEndTime() != null) {
                        if (!s.getStartTime().isAfter(now) && s.getEndTime().isAfter(now)) {
                            anyOngoing = true;
                            break;
                        }
                        if (s.getStartTime().isAfter(now)) {
                            anyFuture = true;
                        }
                    }
                }
                if (anyOngoing) status = "Ongoing";
                else if (anyFuture) status = "Upcoming";
                else status = "Archived";
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
            out.add(map);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/api/admin/shows")
    public ResponseEntity<List<Map<String, Object>>> getShows(@RequestParam("theater_id") Long theaterId,
                                                              @RequestParam("date") String date) {
        List<Object[]> results = showRepository.findShowsByTheaterAndDate(theaterId, date);

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("movieTitle", row[1]);
            map.put("theaterName", row[2]);
            map.put("screen", row[3]);
            map.put("startTime", row[4]);
            map.put("endTime", row[5]);
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/shows")
    public ResponseEntity<String> scheduleShow(@RequestParam("movie_id") Long movieId,
                                               @RequestParam("theater_id") Long theaterId,
                                               @RequestParam("screen") Integer screen,
                                               @RequestParam("start_time") String startTimeStr,
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

            if (screen == null || screen < 1) {
                return ResponseEntity.badRequest().body("Invalid screen number");
            }
            if (theater.getScreenCount() != null && screen > theater.getScreenCount()) {
                return ResponseEntity.badRequest().body("Screen number exceeds theater screen count");
            }

            // parse start/end times. Accept either OffsetDateTime string or local datetime (from datetime-local input)
            java.time.OffsetDateTime start;
            java.time.OffsetDateTime end;
            try {
                if (startTimeStr.endsWith("Z") || startTimeStr.contains("+")) {
                    start = java.time.OffsetDateTime.parse(startTimeStr);
                } else {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(startTimeStr);
                    start = ldt.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
                }
                // compute end time from movie duration
                java.util.Optional<Movie> mOptForDuration = movieRepository.findById(movieId);
                if (mOptForDuration.isEmpty()) return ResponseEntity.badRequest().body("Invalid movie or theater");
                Movie mForDuration = mOptForDuration.get();
                Integer dur = mForDuration.getDurationMinutes();
                if (dur == null) dur = 0;
                end = start.plusMinutes(dur.longValue());
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("Invalid date/time format");
            }

            if (!end.isAfter(start)) {
                return ResponseEntity.badRequest().body("End time must be after start time");
            }

            com.CineBook.model.Show s = new com.CineBook.model.Show();
            s.setMovie(movie);
            s.setTheater(theater);
            s.setScreen(screen);
            s.setStartTime(start);
            s.setEndTime(end);

            showRepository.save(s);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }
}