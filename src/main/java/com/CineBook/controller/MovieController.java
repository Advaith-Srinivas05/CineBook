package com.CineBook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.repository.MovieBookingRepository;
import com.CineBook.repository.UserRepository;
import com.CineBook.model.Movie;
import com.CineBook.model.MovieBooking;
import com.CineBook.model.ShowSchedule;
import com.CineBook.model.Theater;
import com.CineBook.model.User;
import com.CineBook.model.CarouselImage;
import com.CineBook.model.dto.PaymentRequest;
import com.CineBook.model.dto.ShowtimesRequest;
import com.CineBook.service.MovieService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.util.UriUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.Base64;
import java.util.Locale;
import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;

@Controller
public class MovieController {
    private static final int ELITE_SEAT_CAPACITY = 64;
    private static final int NORMAL_SEAT_CAPACITY = 112;
    private static final Set<String> SUPPORTED_BANNER_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/x-webp");
    private static final Set<String> SUPPORTED_BANNER_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private com.CineBook.repository.TheaterRepository theaterRepository;
    
    @Autowired
    private com.CineBook.repository.ShowScheduleRepository showScheduleRepository;

    @Autowired
    private MovieBookingRepository movieBookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieService movieService;

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
                Optional<User> userOpt = getAuthenticatedUser(session);
                if (userOpt.isPresent() && userOpt.get().getLocation() != null && !userOpt.get().getLocation().isBlank()) {
                    String preferredLocation = userOpt.get().getLocation().trim();
                    for (String knownCity : cities) {
                        if (knownCity.equalsIgnoreCase(preferredLocation)) {
                            selectedCity = knownCity;
                            break;
                        }
                    }
                }
            }
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
    public String movieGet(@RequestParam("movieId") Long movieId,
                           @RequestParam(value = "city", required = false) String city,
                           @RequestParam(value = "date", required = false) LocalDate selectedDate,
                           Model model,
                           HttpSession session) {
        return renderMoviePage(movieId, city, selectedDate, model, session);
    }

    @PostMapping("/movie")
    public String moviePost(@RequestParam("movieId") Long movieId,
                            @RequestParam(value = "city", required = false) String city,
                            @RequestParam(value = "date", required = false) LocalDate selectedDate,
                            Model model,
                            HttpSession session) {
        return renderMoviePage(movieId, city, selectedDate, model, session);
    }

    // Shared renderer so both GET and POST can serve the movie details page.
    private String renderMoviePage(Long movieId,
                                   String city,
                                   LocalDate selectedDateInput,
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
            Optional<User> userOpt = getAuthenticatedUser(session);
            if (userOpt.isPresent() && userOpt.get().getLocation() != null && !userOpt.get().getLocation().isBlank()) {
                String preferredLocation = userOpt.get().getLocation().trim();
                for (String knownCity : cities) {
                    if (knownCity.equalsIgnoreCase(preferredLocation)) {
                        selectedCity = knownCity;
                        break;
                    }
                }
            }
        }
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

        LocalDate selectedDate = selectedDateInput == null ? LocalDate.now() : selectedDateInput;
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

            List<String> bookedSeats = movieBookingRepository.findBookedSeatNumbers(schedule.getId(), selectedDate);
            int eliteBookedSeats = 0;
            int normalBookedSeats = 0;
            for (String seatNumber : bookedSeats) {
                if (movieService.isEliteSeat(seatNumber)) {
                    eliteBookedSeats++;
                } else {
                    normalBookedSeats++;
                }
            }

            TierStatus normalTierStatus = resolveTierStatus(normalBookedSeats, NORMAL_SEAT_CAPACITY);
            TierStatus eliteTierStatus = resolveTierStatus(eliteBookedSeats, ELITE_SEAT_CAPACITY);
            boolean bookingOpen = isBookingOpen(schedule, selectedDate);

            theaterShow.getShows().add(new ShowTimeView(
                    schedule.getId(),
                    schedule.getStartTime(),
                    schedule.getTheater().getPrice(),
                    schedule.getTheater().getElitePrice(),
                    normalTierStatus.getLabel(),
                    normalTierStatus.getCssClass(),
                    eliteTierStatus.getLabel(),
                    eliteTierStatus.getCssClass(),
                    bookingOpen
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

    // Movie -> showtimes transition now uses POST with form data.
    @PostMapping("/showtimes")
    public String showtimes(@ModelAttribute ShowtimesRequest request,
                            Model model,
                            HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        if (getAuthenticatedUser(session).isEmpty()) return "redirect:/?auth=login";

        if (request.getMovieId() == null || request.getTheaterId() == null || request.getScheduleId() == null) {
            return "redirect:/";
        }

        Optional<ShowSchedule> scheduleOpt = resolveSchedule(request.getMovieId(), request.getTheaterId(), request.getScheduleId());
        if (scheduleOpt.isEmpty()) {
            return "redirect:/";
        }

        ShowSchedule schedule = scheduleOpt.get();
        LocalDate showDate = request.getSelectedDate() == null ? LocalDate.now() : request.getSelectedDate();
        if (showDate.isBefore(schedule.getStartDate()) || showDate.isAfter(schedule.getEndDate())) {
            showDate = schedule.getStartDate();
        }
        if (!isBookingOpen(schedule, showDate)) {
            return "redirect:/";
        }

        populateBookingContext(model, schedule, showDate, null, Collections.emptyList());
        return "showtimes";
    }

    @PostMapping("/seats")
    public String seats(@RequestParam("movieId") Long movieId,
                        @RequestParam("theaterId") Long theaterId,
                        @RequestParam("scheduleId") Long scheduleId,
                        @RequestParam(value = "showDate", required = false) LocalDate showDateInput,
                        @RequestParam(value = "ticketCount", required = false, defaultValue = "1") Integer ticketCount,
                        Model model,
                        HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        if (getAuthenticatedUser(session).isEmpty()) return "redirect:/?auth=login";

        Optional<ShowSchedule> scheduleOpt = resolveSchedule(movieId, theaterId, scheduleId);
        if (scheduleOpt.isEmpty()) {
            return "redirect:/";
        }

        ShowSchedule schedule = scheduleOpt.get();
        LocalDate showDate = showDateInput == null ? LocalDate.now() : showDateInput;
        if (showDate.isBefore(schedule.getStartDate()) || showDate.isAfter(schedule.getEndDate())) {
            showDate = schedule.getStartDate();
        }
        if (!isBookingOpen(schedule, showDate)) {
            return "redirect:/";
        }

        int boundedTicketCount = ticketCount == null ? 1 : Math.max(1, Math.min(ticketCount, 10));
        List<String> bookedSeats = movieBookingRepository.findBookedSeatNumbers(scheduleId, showDate).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.toList());

        populateBookingContext(model, schedule, showDate, boundedTicketCount, bookedSeats);
        return "seats";
    }

    // Seats -> payment transition now uses POST /payments.
    @PostMapping("/payments")
    public String payments(@ModelAttribute PaymentRequest request,
                           Model model,
                           HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";

        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) return "redirect:/?auth=login";

        if (request.getMovieId() == null || request.getTheaterId() == null || request.getScheduleId() == null) {
            return "redirect:/";
        }

        Optional<ShowSchedule> scheduleOpt = resolveSchedule(request.getMovieId(), request.getTheaterId(), request.getScheduleId());
        if (scheduleOpt.isEmpty()) return "redirect:/";

        ShowSchedule schedule = scheduleOpt.get();
        LocalDate showDate = request.getShowDate() == null ? LocalDate.now() : request.getShowDate();
        if (showDate.isBefore(schedule.getStartDate()) || showDate.isAfter(schedule.getEndDate())) {
            showDate = schedule.getStartDate();
        }
        if (!isBookingOpen(schedule, showDate)) {
            return "redirect:/";
        }

        int boundedTicketCount = request.getTicketCount() == null ? 1 : Math.max(1, Math.min(request.getTicketCount(), 10));
        List<String> normalizedSelectedSeats = movieService.parseSeatNumbers(request.getSelectedSeats());
        Set<String> alreadyBooked = movieBookingRepository.findBookedSeatNumbers(request.getScheduleId(), showDate).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (normalizedSelectedSeats.size() != boundedTicketCount) {
            populateBookingContext(model, schedule, showDate, boundedTicketCount, new ArrayList<>(alreadyBooked));
            model.addAttribute("seatSelectionError", "Please select exactly " + boundedTicketCount + " seats.");
            return "seats";
        }

        boolean hasBookedSeat = normalizedSelectedSeats.stream().anyMatch(alreadyBooked::contains);
        if (hasBookedSeat) {
            populateBookingContext(model, schedule, showDate, boundedTicketCount, new ArrayList<>(alreadyBooked));
            model.addAttribute("seatSelectionError", "One or more selected seats have already been booked.");
            return "seats";
        }

        populateBookingContext(model, schedule, showDate, boundedTicketCount, new ArrayList<>(alreadyBooked));
        model.addAttribute("selectedSeats", String.join(",", normalizedSelectedSeats));
        int eliteSeatCount = (int) normalizedSelectedSeats.stream().filter(movieService::isEliteSeat).count();
        int normalSeatCount = boundedTicketCount - eliteSeatCount;

        model.addAttribute("ticketPrice", movieService.resolveTicketPrice(schedule.getTheater()));
        model.addAttribute("eliteTicketPrice", movieService.resolveEliteTicketPrice(schedule.getTheater()));
        model.addAttribute("normalSeatCount", normalSeatCount);
        model.addAttribute("eliteSeatCount", eliteSeatCount);
        model.addAttribute("totalAmount", movieService.calculateBookingTotal(normalizedSelectedSeats, schedule.getTheater()));
        return "payment";
    }

    @PostMapping("/payments/confirm")
    @Transactional
    public String confirmPayment(@ModelAttribute PaymentRequest request,
                                 HttpSession session,
                                 Model model) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";

        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) return "redirect:/?auth=login";

        if (request.getMovieId() == null || request.getTheaterId() == null || request.getScheduleId() == null) {
            return "redirect:/";
        }

        Optional<ShowSchedule> scheduleOpt = resolveSchedule(request.getMovieId(), request.getTheaterId(), request.getScheduleId());
        if (scheduleOpt.isEmpty()) return "redirect:/";

        ShowSchedule schedule = scheduleOpt.get();
        LocalDate showDate = request.getShowDate() == null ? LocalDate.now() : request.getShowDate();
        if (showDate.isBefore(schedule.getStartDate()) || showDate.isAfter(schedule.getEndDate())) {
            showDate = schedule.getStartDate();
        }
        if (!isBookingOpen(schedule, showDate)) {
            return "redirect:/";
        }

        int boundedTicketCount = request.getTicketCount() == null ? 1 : Math.max(1, Math.min(request.getTicketCount(), 10));
        try {
            String publicId = movieService.createBooking(
                    userOpt.get(),
                    schedule,
                    showDate,
                    boundedTicketCount,
                    request.getSelectedSeats()
            );
            return "redirect:/ticket/" + publicId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            List<String> normalizedSelectedSeats = movieService.parseSeatNumbers(request.getSelectedSeats());
            Set<String> alreadyBooked = movieBookingRepository.findBookedSeatNumbers(request.getScheduleId(), showDate).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            populateBookingContext(model, schedule, showDate, boundedTicketCount, new ArrayList<>(alreadyBooked));
            model.addAttribute("selectedSeats", String.join(",", normalizedSelectedSeats));
            int eliteSeatCount = (int) normalizedSelectedSeats.stream().filter(movieService::isEliteSeat).count();
            int normalSeatCount = Math.max(0, boundedTicketCount - eliteSeatCount);
            model.addAttribute("normalSeatCount", normalSeatCount);
            model.addAttribute("eliteSeatCount", eliteSeatCount);
            model.addAttribute("totalAmount", movieService.calculateBookingTotal(normalizedSelectedSeats, schedule.getTheater()));
            model.addAttribute("paymentError", ex.getMessage());
            return "payment";
        }
    }

    // Public ticket endpoint uses UUID-based public id, never the internal DB id.
    @GetMapping("/ticket/{publicId}")
    public String ticket(@PathVariable("publicId") String publicId,
                         HttpSession session,
                         Model model) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";

        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) return "redirect:/?auth=login";

        Optional<MovieBooking> bookingOpt = movieBookingRepository.findByPublicId(publicId);
        if (bookingOpt.isEmpty() || bookingOpt.get().getUser() == null
                || !bookingOpt.get().getUser().getId().equals(userOpt.get().getId())) {
            model.addAttribute("ticketError", "Ticket not found or access denied.");
            return "ticket-error";
        }

        MovieBooking booking = bookingOpt.get();
        ShowSchedule schedule = booking.getShow();
        populateBookingContext(model, schedule, booking.getShowDate(), booking.getSeatCount(), Collections.emptyList());
        model.addAttribute("selectedSeats", booking.getSeatNumbers());
        model.addAttribute("publicBookingId", booking.getPublicId());

        Integer seatCountObj = booking.getSeatCount();
        int seatCount = seatCountObj == null ? 1 : seatCountObj;
        List<String> bookingSeats = movieService.parseSeatNumbers(booking.getSeatNumbers());
        int eliteSeatCount = 0;
        for (String seat : bookingSeats) {
            if (movieService.isEliteSeat(seat)) {
                eliteSeatCount++;
            }
        }

        int normalSeatCount = Math.max(0, seatCount - eliteSeatCount);
        model.addAttribute("normalSeatCount", normalSeatCount);
        model.addAttribute("eliteSeatCount", eliteSeatCount);
        model.addAttribute("ticketPrice", movieService.resolveTicketPrice(schedule.getTheater()));
        model.addAttribute("eliteTicketPrice", movieService.resolveEliteTicketPrice(schedule.getTheater()));
        Integer totalPriceObj = booking.getTotalPrice();
        model.addAttribute("totalAmount", totalPriceObj == null ? 0 : totalPriceObj);
        return "ticket";
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
    public String admin(HttpSession session, Model model) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            model.addAttribute("adminBanners", buildAdminBannerData());
            return "admin";
        }
        if (username != null) {
            return "redirect:/"; // logged-in non-admin users -> index
        }
        String returnTo = UriUtils.encode("/admin", StandardCharsets.UTF_8);
        return "redirect:/?auth=login&returnTo=" + returnTo; // not logged in -> auth modal
    }

    @GetMapping("/api/admin/banners")
    public ResponseEntity<List<Map<String, Object>>> getAdminBanners(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(buildAdminBannerData());
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

            // prevent duplicate theater names
            if (theaterRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(409).body("Theater with this name already exists");
            }
            com.CineBook.model.Theater t = new com.CineBook.model.Theater();
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
    public ResponseEntity<String> updateTheater(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
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
    public ResponseEntity<String> deleteTheater(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
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
            map.put("price", t.getPrice());
            map.put("elitePrice", t.getElitePrice());
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
        private final Integer ticketPrice;
        private final Integer eliteTicketPrice;
        private final String normalStatusLabel;
        private final String normalStatusClass;
        private final String eliteStatusLabel;
        private final String eliteStatusClass;
        private final boolean bookingOpen;

        public ShowTimeView(Long scheduleId,
                            LocalTime startTime,
                            Integer ticketPrice,
                            Integer eliteTicketPrice,
                            String normalStatusLabel,
                            String normalStatusClass,
                            String eliteStatusLabel,
                            String eliteStatusClass,
                            boolean bookingOpen) {
            this.scheduleId = scheduleId;
            this.startTime = startTime;
            this.ticketPrice = ticketPrice;
            this.eliteTicketPrice = eliteTicketPrice;
            this.normalStatusLabel = normalStatusLabel;
            this.normalStatusClass = normalStatusClass;
            this.eliteStatusLabel = eliteStatusLabel;
            this.eliteStatusClass = eliteStatusClass;
            this.bookingOpen = bookingOpen;
        }

        public Long getScheduleId() {
            return scheduleId;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public Integer getTicketPrice() {
            return ticketPrice;
        }

        public Integer getEliteTicketPrice() {
            return eliteTicketPrice;
        }

        public String getNormalStatusLabel() {
            return normalStatusLabel;
        }

        public String getNormalStatusClass() {
            return normalStatusClass;
        }

        public String getEliteStatusLabel() {
            return eliteStatusLabel;
        }

        public String getEliteStatusClass() {
            return eliteStatusClass;
        }

        public boolean isBookingOpen() {
            return bookingOpen;
        }
    }

    private static class TierStatus {
        private final String label;
        private final String cssClass;

        private TierStatus(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }

        public String getLabel() {
            return label;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private TierStatus resolveTierStatus(int bookedSeats, int totalSeats) {
        if (totalSeats <= 0) {
            return new TierStatus("Sold Out", "sold-out");
        }

        int remainingSeats = Math.max(0, totalSeats - Math.max(0, bookedSeats));
        if (remainingSeats == 0) {
            return new TierStatus("Sold Out", "sold-out");
        }

        double remainingRatio = remainingSeats / (double) totalSeats;
        if (remainingRatio <= 0.25d) {
            return new TierStatus("Filling Fast", "filling");
        }

        return new TierStatus("Available", "available");
    }

    private boolean isBookingOpen(ShowSchedule schedule, LocalDate showDate) {
        if (schedule == null || showDate == null || schedule.getStartTime() == null) {
            return false;
        }

        LocalDateTime cutoffTime = LocalDateTime.of(showDate, schedule.getStartTime()).minusHours(1);
        return LocalDateTime.now().isBefore(cutoffTime);
    }

    private Optional<ShowSchedule> resolveSchedule(Long movieId, Long theaterId, Long scheduleId) {
        Optional<ShowSchedule> scheduleOpt = showScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return Optional.empty();
        }

        ShowSchedule schedule = scheduleOpt.get();
        if (schedule.getMovie() == null || schedule.getTheater() == null) {
            return Optional.empty();
        }

        if (!schedule.getMovie().getId().equals(movieId) || !schedule.getTheater().getId().equals(theaterId)) {
            return Optional.empty();
        }

        return Optional.of(schedule);
    }

    private Optional<User> getAuthenticatedUser(HttpSession session) {
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr instanceof Number number) {
            Long userId = number.longValue();
            Optional<User> userById = userRepository.findById(userId);
            if (userById.isPresent()) {
                return userById;
            }
        }

        Object usernameAttr = session.getAttribute("username");
        if (!(usernameAttr instanceof String username) || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    private void populateBookingContext(Model model,
                                        ShowSchedule schedule,
                                        LocalDate showDate,
                                        Integer ticketCount,
                                        List<String> bookedSeats) {
        Movie movie = schedule.getMovie();
        Theater theater = schedule.getTheater();

        model.addAttribute("movie", movie);
        model.addAttribute("theater", theater);
        model.addAttribute("schedule", schedule);
        model.addAttribute("showDate", showDate);
        model.addAttribute("ticketCount", ticketCount == null ? 1 : ticketCount);
        model.addAttribute("ticketPrice", movieService.resolveTicketPrice(theater));
        model.addAttribute("eliteTicketPrice", movieService.resolveEliteTicketPrice(theater));
        model.addAttribute("bookedSeats", bookedSeats == null ? Collections.emptyList() : bookedSeats);
    }
}