package com.CineBook.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private boolean success;

    private Instant timestamp = Instant.now();

    public LoginAttempt() {}

    public LoginAttempt(String username, boolean success) {
        this.username = username;
        this.success = success;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
