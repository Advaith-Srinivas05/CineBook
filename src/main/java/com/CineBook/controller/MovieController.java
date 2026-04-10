package com.CineBook.controller;

import com.CineBook.model.Movie;
import com.CineBook.model.ShowSchedule;
import com.CineBook.model.User;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieBookingRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.repository.ShowScheduleRepository;
import com.CineBook.repository.TheaterRepository;
import com.CineBook.repository.UserRepository;
import com.CineBook.service.MovieService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class MovieController {
    private static final int ELITE_SEAT_CAPACITY = 64;
    private static final int NORMAL_SEAT_CAPACITY = 112;

    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowScheduleRepository showScheduleRepository;

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
        if (isAdmin(session)) {
            return "redirect:/admin";
        }
        try {
            model.addAttribute("carouselImages", repository.findAllProjectedBy());

            List<String> cities = theaterRepository.findDistinctCities();
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
        Optional<Movie> movieOpt = movieRepository.findById(id);
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

    private String renderMoviePage(Long movieId,
                                   String city,
                                   LocalDate selectedDateInput,
                                   Model model,
                                   HttpSession session) {
        if (isAdmin(session)) return "redirect:/admin";

        Optional<Movie> movieOpt = movieRepository.findById(movieId);
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

    private boolean isAdmin(HttpSession session) {
        return getAuthenticatedUser(session)
                .map(User::getRole)
                .filter(role -> role == User.Role.ADMIN)
                .isPresent();
    }
}
