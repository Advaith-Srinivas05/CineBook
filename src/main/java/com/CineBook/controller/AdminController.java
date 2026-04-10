package com.CineBook.controller;

import com.CineBook.model.CarouselImage;
import com.CineBook.model.Movie;
import com.CineBook.model.ShowSchedule;
import com.CineBook.model.Theater;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.repository.ShowScheduleRepository;
import com.CineBook.repository.TheaterRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class AdminController {
    private static final Set<String> SUPPORTED_BANNER_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/x-webp");
    private static final Set<String> SUPPORTED_BANNER_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowScheduleRepository showScheduleRepository;

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            model.addAttribute("adminBanners", buildAdminBannerData());
            return "admin";
        }
        if (username != null) {
            return "redirect:/";
        }
        String returnTo = UriUtils.encode("/admin", StandardCharsets.UTF_8);
        return "redirect:/?auth=login&returnTo=" + returnTo;
    }

    @GetMapping("/api/admin/banners")
    public ResponseEntity<List<Map<String, Object>>> getAdminBanners(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(buildAdminBannerData());
    }

    @PostMapping("/admin/banners")
    public ResponseEntity<String> uploadAdminBanner(@RequestParam("banner") MultipartFile banner,
                                                    HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        if (banner == null || banner.isEmpty()) {
            return ResponseEntity.badRequest().body("Banner image is required");
        }

        String contentType = banner.getContentType();
        boolean supportedMimeType = isSupportedBannerMimeType(contentType);
        boolean supportedExtension = isSupportedBannerExtension(banner.getOriginalFilename());
        if (!supportedMimeType && !supportedExtension) {
            return ResponseEntity.badRequest().body("Only PNG, JPEG, or WebP images are allowed");
        }

        if (!supportedExtension) {
            return ResponseEntity.badRequest().body("Unsupported file extension. Allowed: .png, .jpg, .jpeg, .webp");
        }

        try {
            byte[] imageBytes = banner.getBytes();
            BannerMetadata metadata = extractBannerMetadata(imageBytes, banner.getOriginalFilename(), contentType);
            CarouselImage entity = new CarouselImage();
            String originalName = banner.getOriginalFilename();
            entity.setImageName((originalName == null || originalName.isBlank()) ? "banner-image" : originalName.trim());
            entity.setImageData(imageBytes);
            entity.setImageSizeBytes(metadata.sizeBytes);
            entity.setImageWidth(metadata.width);
            entity.setImageHeight(metadata.height);
            entity.setAspectRatio(metadata.aspectRatio);
            entity.setFileType(metadata.fileType);
            repository.save(entity);
            return ResponseEntity.ok("OK");
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to read uploaded image");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @DeleteMapping("/admin/banners/{id}")
    public ResponseEntity<String> deleteAdminBanner(@PathVariable("id") Long id,
                                                    HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        if (!repository.existsById(id)) {
            return ResponseEntity.status(404).body("Banner not found");
        }

        try {
            repository.deleteById(id);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
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
    public ResponseEntity<List<Map<String, Object>>> listAllMovies(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).build();
        }
        List<Movie> list = movieRepository.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        for (Movie m : list) {
            Map<String, Object> map = new HashMap<>();
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
    public ResponseEntity<String> updateMovie(@PathVariable("id") Long id,
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
            Optional<Movie> mOpt = movieRepository.findById(id);
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
    public ResponseEntity<String> deleteMovie(@PathVariable("id") Long id,
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
                                             @RequestParam(value = "price", required = false) Integer price,
                                             @RequestParam(value = "elite_price", required = false) Integer elitePrice,
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
            if (price == null || price < 1) {
                return ResponseEntity.badRequest().body("Ticket price must be at least 1");
            }
            if (elitePrice == null || elitePrice < 1) {
                return ResponseEntity.badRequest().body("Elite ticket price must be at least 1");
            }

            if (theaterRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(409).body("Theater with this name already exists");
            }
            Theater t = new Theater();
            t.setName(name);
            t.setCity(city);
            t.setLocation(location);
            t.setScreenCount(screenCount == null ? 1 : screenCount);
            t.setPrice(price);
            t.setElitePrice(elitePrice);
            theaterRepository.save(t);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @GetMapping("/api/admin/theaters")
    public ResponseEntity<List<Map<String, Object>>> getAdminTheaters(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).build();
        }

        List<Theater> theaters = theaterRepository.findAll();
        theaters.sort(Comparator.comparing(Theater::getId));

        List<Map<String, Object>> out = new ArrayList<>();
        for (Theater t : theaters) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("city", t.getCity());
            map.put("location", t.getLocation());
            map.put("screenCount", t.getScreenCount());
            map.put("price", t.getPrice());
            map.put("elitePrice", t.getElitePrice());
            out.add(map);
        }

        return ResponseEntity.ok(out);
    }

    @PostMapping(value = "/admin/theaters/{id}/update", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> updateTheater(@PathVariable("id") Long id,
                                                @RequestParam("name") String name,
                                                @RequestParam("city") String city,
                                                @RequestParam(value = "location", required = false) String location,
                                                @RequestParam(value = "screen_count", required = false) Integer screenCount,
                                                @RequestParam(value = "price", required = false) Integer price,
                                                @RequestParam(value = "elite_price", required = false) Integer elitePrice,
                                                HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            Optional<Theater> theaterOpt = theaterRepository.findById(id);
            if (theaterOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Theater not found");
            }

            name = name == null ? "" : name.trim();
            city = city == null ? "" : city.trim();
            location = location == null ? null : location.trim();

            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body("Theater name is required");
            }
            if (city.isEmpty()) {
                return ResponseEntity.badRequest().body("City is required");
            }
            if (screenCount == null || screenCount < 1) {
                return ResponseEntity.badRequest().body("Screen count must be at least 1");
            }
            if (price == null || price < 1) {
                return ResponseEntity.badRequest().body("Ticket price must be at least 1");
            }
            if (elitePrice == null || elitePrice < 1) {
                return ResponseEntity.badRequest().body("Elite ticket price must be at least 1");
            }

            Optional<Theater> sameName = theaterRepository.findByName(name);
            if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
                return ResponseEntity.status(409).body("Theater with this name already exists");
            }

            if (showScheduleRepository.existsByTheaterIdAndScreenGreaterThan(id, screenCount)) {
                return ResponseEntity.badRequest().body("Cannot reduce screen count below existing scheduled screen numbers");
            }

            Theater theater = theaterOpt.get();
            theater.setName(name);
            theater.setCity(city);
            theater.setLocation(location);
            theater.setScreenCount(screenCount);
            theater.setPrice(price);
            theater.setElitePrice(elitePrice);
            theaterRepository.save(theater);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @PostMapping("/admin/theaters/{id}/delete")
    public ResponseEntity<String> deleteTheater(@PathVariable("id") Long id,
                                                HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            if (!theaterRepository.existsById(id)) {
                return ResponseEntity.status(404).body("Theater not found");
            }
            theaterRepository.deleteById(id);
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
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
    public ResponseEntity<String> updateScheduledSlot(@PathVariable("id") Long scheduleId,
                                                      @RequestParam("start_date") String startDateStr,
                                                      @RequestParam("end_date") String endDateStr,
                                                      @RequestParam("screen") Integer screen,
                                                      HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Optional<ShowSchedule> scheduleOpt = showScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Show schedule not found");
        }

        ShowSchedule schedule = scheduleOpt.get();
        Movie movie = schedule.getMovie();
        Theater theater = schedule.getTheater();

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
            Optional<Movie> mOpt = movieRepository.findById(movieId);
            Optional<Theater> tOpt = theaterRepository.findById(theaterId);
            if (mOpt.isEmpty() || tOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid movie or theater");
            }

            Movie movie = mOpt.get();
            Theater theater = tOpt.get();

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
    public ResponseEntity<String> deleteScheduledSlot(@PathVariable("id") Long scheduleId,
                                                      HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Optional<ShowSchedule> scheduleOpt = showScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }

        showScheduleRepository.delete(scheduleOpt.get());
        return ResponseEntity.ok("OK");
    }

    private List<Map<String, Object>> buildAdminBannerData() {
        List<CarouselImage> banners = repository.findAll();
        banners.sort(Comparator.comparing(CarouselImage::getId));

        List<Map<String, Object>> out = new ArrayList<>();
        for (CarouselImage banner : banners) {
            byte[] imageData = banner.getImageData();
            Long sizeBytes = banner.getImageSizeBytes();
            Integer width = banner.getImageWidth();
            Integer height = banner.getImageHeight();
            String aspectRatio = banner.getAspectRatio();
            String fileType = banner.getFileType();

            if (imageData != null && (sizeBytes == null || width == null || height == null || aspectRatio == null || aspectRatio.isBlank() || fileType == null || fileType.isBlank())) {
                BannerMetadata metadata = extractBannerMetadata(imageData, banner.getImageName(), null);
                sizeBytes = sizeBytes == null ? metadata.sizeBytes : sizeBytes;
                width = width == null ? metadata.width : width;
                height = height == null ? metadata.height : height;
                aspectRatio = (aspectRatio == null || aspectRatio.isBlank()) ? metadata.aspectRatio : aspectRatio;
                fileType = (fileType == null || fileType.isBlank()) ? metadata.fileType : fileType;
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", banner.getId());
            map.put("imageName", banner.getImageName());
            String imageBase64 = imageData == null ? "" : Base64.getEncoder().encodeToString(imageData);
            String mimeType = resolveMimeType(fileType);
            map.put("imageBase64", imageBase64);
            map.put("imageDataUri", imageBase64.isBlank() ? "" : "data:" + mimeType + ";base64," + imageBase64);
            map.put("imageSizeBytes", sizeBytes);
            map.put("imageSizeDisplay", humanReadableSize(sizeBytes));
            map.put("width", width);
            map.put("height", height);
            map.put("dimensions", buildDimensions(width, height));
            map.put("aspectRatio", aspectRatio == null || aspectRatio.isBlank() ? "-" : aspectRatio);
            map.put("fileType", fileType == null || fileType.isBlank() ? "-" : fileType.toUpperCase(Locale.ROOT));
            map.put("fileTypeValue", normalizeFileType(fileType));
            out.add(map);
        }

        return out;
    }

    private static String humanReadableSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes < 0) return "-";
        if (sizeBytes < 1024) return sizeBytes + " B";

        double kb = sizeBytes / 1024.0;
        if (kb < 1024) {
            if (kb < 10) return String.format(Locale.ROOT, "%.1f KB", kb);
            return String.format(Locale.ROOT, "%.0f KB", kb);
        }

        double mb = kb / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    private static String buildDimensions(Integer width, Integer height) {
        if (width == null || height == null || width <= 0 || height <= 0) return "-";
        return width + " x " + height;
    }

    private static BannerMetadata extractBannerMetadata(byte[] imageBytes, String fileName, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Banner image is required");
        }

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read image dimensions");
        }

        if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
            throw new IllegalArgumentException("Unsupported or unreadable image file. Allowed: PNG, JPEG, WebP");
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int gcd = greatestCommonDivisor(width, height);
        String ratio = (width / gcd) + ":" + (height / gcd);

        return new BannerMetadata(
                (long) imageBytes.length,
                width,
                height,
                ratio,
                extractFileType(fileName, contentType)
        );
    }

    private static String extractFileType(String fileName, String contentType) {
        String fromMime = normalizeFileType(contentType);
        if (!"unknown".equals(fromMime)) {
            return fromMime;
        }

        if (fileName == null) return "unknown";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "unknown";
        return normalizeFileType(fileName.substring(dot + 1));
    }

    private static String resolveMimeType(String fileType) {
        if (fileType == null) return "image/png";
        String normalized = fileType.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "webp":
                return "image/webp";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
            case "svg+xml":
                return "image/svg+xml";
            case "png":
            default:
                return "image/png";
        }
    }

    private static boolean isSupportedBannerMimeType(String contentType) {
        if (contentType == null) return false;
        return SUPPORTED_BANNER_MIME_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isSupportedBannerExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return false;
        String ext = fileName.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_BANNER_EXTENSIONS.contains(ext);
    }

    private static String normalizeFileType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "unknown";
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("/")) {
            normalized = normalized.substring(normalized.indexOf('/') + 1).trim();
        }
        if (normalized.contains(";")) {
            normalized = normalized.substring(0, normalized.indexOf(';')).trim();
        }

        if ("jpg".equals(normalized)) return "jpeg";
        if ("x-webp".equals(normalized)) return "webp";
        if (Arrays.asList("jpeg", "png", "webp", "gif", "bmp", "svg+xml", "svg").contains(normalized)) {
            return normalized;
        }
        return "unknown";
    }

    private static int greatestCommonDivisor(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x == 0 ? 1 : x;
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

    private static class BannerMetadata {
        private final long sizeBytes;
        private final int width;
        private final int height;
        private final String aspectRatio;
        private final String fileType;

        private BannerMetadata(long sizeBytes, int width, int height, String aspectRatio, String fileType) {
            this.sizeBytes = sizeBytes;
            this.width = width;
            this.height = height;
            this.aspectRatio = aspectRatio;
            this.fileType = fileType;
        }
    }
}
