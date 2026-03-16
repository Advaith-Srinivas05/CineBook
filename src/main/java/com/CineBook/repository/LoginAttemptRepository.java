package com.CineBook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.CineBook.model.LoginAttempt;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
}
