package com.CineBook.controller;

import com.CineBook.model.Movie;
import com.CineBook.model.MovieBooking;
import com.CineBook.model.ShowSchedule;
import com.CineBook.model.Theater;
import com.CineBook.model.User;
import com.CineBook.model.dto.PaymentRequest;
import com.CineBook.model.dto.ShowtimesRequest;
import com.CineBook.repository.MovieBookingRepository;
import com.CineBook.repository.ShowScheduleRepository;
import com.CineBook.repository.UserRepository;
import com.CineBook.service.MovieService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    @Autowired
    private ShowScheduleRepository showScheduleRepository;

    @Autowired
    private MovieBookingRepository movieBookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieService movieService;

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
    public String ticket(@org.springframework.web.bind.annotation.PathVariable("publicId") String publicId,
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
