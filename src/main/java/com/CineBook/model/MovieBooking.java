package com.CineBook.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movie_bookings")
public class MovieBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "show_id", nullable = false)
    private ShowSchedule show;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "seat_numbers", nullable = false, columnDefinition = "TEXT")
    private String seatNumbers;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (publicId == null || publicId.isBlank()) {
            publicId = UUID.randomUUID().toString();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShowSchedule getShow() {
        return show;
    }

    public void setShow(ShowSchedule show) {
        this.show = show;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MovieBooking booking = new MovieBooking();

        public Builder show(ShowSchedule show) {
            booking.setShow(show);
            return this;
        }

        public Builder user(User user) {
            booking.setUser(user);
            return this;
        }

        public Builder showDate(LocalDate showDate) {
            booking.setShowDate(showDate);
            return this;
        }

        public Builder seatCount(Integer seatCount) {
            booking.setSeatCount(seatCount);
            return this;
        }

        public Builder totalPrice(Integer totalPrice) {
            booking.setTotalPrice(totalPrice);
            return this;
        }

        public Builder seatNumbers(String seatNumbers) {
            booking.setSeatNumbers(seatNumbers);
            return this;
        }

        public Builder publicId(String publicId) {
            booking.setPublicId(publicId);
            return this;
        }

        public MovieBooking build() {
            return booking;
        }
    }
}
