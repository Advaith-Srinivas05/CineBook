package com.CineBook.controller;

import com.CineBook.model.User;
import com.CineBook.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ControllerAdvice
public class GlobalUserModelAdvice {

    private static final String DEFAULT_AVATAR = "/images/avatar.png";

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("currentUserProfileImage")
    public String currentUserProfileImage(HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return DEFAULT_AVATAR;
        }
        return resolveProfileImageUrl(userOpt.get().getProfileImage());
    }

    @ModelAttribute("currentUserRecord")
    public User currentUserRecord(HttpSession session) {
        return getAuthenticatedUser(session).orElse(null);
    }

    private Optional<User> getAuthenticatedUser(HttpSession session) {
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr instanceof Number number) {
            Optional<User> byId = userRepository.findById(number.longValue());
            if (byId.isPresent()) {
                return byId;
            }
        }

        Object usernameAttr = session.getAttribute("username");
        if (!(usernameAttr instanceof String username) || username.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username);
    }

    public static String resolveProfileImageUrl(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            return DEFAULT_AVATAR;
        }

        String value = profileImage.trim();
        if (value.startsWith("/") || value.startsWith("data:")) {
            return value;
        }

        return "/images/uploads/" + UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
