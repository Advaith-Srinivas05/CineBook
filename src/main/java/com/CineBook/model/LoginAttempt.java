package com.CineBook.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    public LoginAttempt() {}

    public LoginAttempt(String username, boolean success) {
        this(username, null, success);
    }

    public LoginAttempt(String username, Long userId, boolean success) {
        this.username = username;
        this.userId = userId;
        this.success = success;
        this.attemptedAt = Instant.now();
    }

    @PrePersist
    public void onCreate() {
        if (attemptedAt == null) {
            attemptedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Instant getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Instant attemptedAt) { this.attemptedAt = attemptedAt; }

    // Backward-compatible accessors in case any existing code still uses timestamp naming.
    public Instant getTimestamp() { return attemptedAt; }
    public void setTimestamp(Instant timestamp) { this.attemptedAt = timestamp; }
}
