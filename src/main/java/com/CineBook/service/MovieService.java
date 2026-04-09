package com.CineBook.service;

import com.CineBook.model.MovieBooking;
import com.CineBook.model.ShowSchedule;
import com.CineBook.model.Theater;
import com.CineBook.model.User;
import com.CineBook.repository.MovieBookingRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MovieService {

	@Autowired
	private MovieBookingRepository movieBookingRepository;

	@Transactional
	public String createBooking(User user,
								ShowSchedule schedule,
								LocalDate showDate,
								Integer ticketCount,
								String selectedSeatsRaw) {
		if (user == null || schedule == null || showDate == null) {
			throw new IllegalArgumentException("Booking details are incomplete.");
		}
		if (schedule.getTheater() == null) {
			throw new IllegalArgumentException("Theater details are missing for this show.");
		}

		int boundedTicketCount = ticketCount == null ? 1 : Math.max(1, Math.min(ticketCount, 10));
		List<String> selectedSeats = parseSeatNumbers(selectedSeatsRaw);
		if (selectedSeats.size() != boundedTicketCount) {
			throw new IllegalArgumentException("Selected seats do not match the requested ticket count.");
		}

		Set<String> alreadyBooked = movieBookingRepository
				.findBookedSeatNumbers(schedule.getId(), showDate)
				.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(String::toUpperCase)
				.collect(Collectors.toSet());
		boolean hasBookedSeat = selectedSeats.stream().anyMatch(alreadyBooked::contains);
		if (hasBookedSeat) {
			throw new IllegalStateException("One or more selected seats were already booked.");
		}

		MovieBooking booking = new MovieBooking();
		booking.setShow(schedule);
		booking.setUser(user);
		booking.setShowDate(showDate);
		booking.setSeatCount(boundedTicketCount);
		booking.setTotalPrice(calculateBookingTotal(selectedSeats, schedule.getTheater()));
		booking.setSeatNumbers(String.join(",", selectedSeats));
		booking.setPublicId(generatePublicId());

		MovieBooking savedBooking = movieBookingRepository.save(booking);
		return savedBooking.getPublicId();
	}

	public String generatePublicId() {
		return UUID.randomUUID().toString();
	}

	public List<String> parseSeatNumbers(String seatNumbers) {
		if (seatNumbers == null || seatNumbers.isBlank()) {
			return Collections.emptyList();
		}

		Set<String> uniqueSeats = java.util.Arrays.stream(seatNumbers.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(String::toUpperCase)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return new ArrayList<>(uniqueSeats);
	}

	public int calculateBookingTotal(List<String> seatNumbers, Theater theater) {
		int ticketPrice = resolveTicketPrice(theater);
		int eliteTicketPrice = resolveEliteTicketPrice(theater);

		int totalAmount = 0;
		for (String seat : seatNumbers) {
			totalAmount += isEliteSeat(seat) ? eliteTicketPrice : ticketPrice;
		}
		return totalAmount;
	}

	public int resolveTicketPrice(Theater theater) {
		if (theater == null || theater.getPrice() == null || theater.getPrice() < 1) {
			return 250;
		}
		return theater.getPrice();
	}

	public int resolveEliteTicketPrice(Theater theater) {
		if (theater == null || theater.getElitePrice() == null || theater.getElitePrice() < 1) {
			return 350;
		}
		return theater.getElitePrice();
	}

	public boolean isEliteSeat(String seatNumber) {
		if (seatNumber == null || seatNumber.isBlank()) {
			return false;
		}
		char row = Character.toUpperCase(seatNumber.trim().charAt(0));
		return row >= 'A' && row <= 'D';
	}
}