package com.CineBook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.CineBook.repository.UserRepository;
import com.CineBook.repository.LoginAttemptRepository;
import com.CineBook.model.User;
import com.CineBook.model.LoginAttempt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UriComponentsBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          @RequestParam(value = "returnTo", required = false) String returnTo,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        Optional<User> maybeUser = userRepository.findByEmail(normalizedEmail);
        Long loginUserId = maybeUser.map(User::getId).orElse(null);
        boolean success = false;
        boolean isAdmin = false;
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            String hash = hash(password);
            if (hash != null && hash.equals(user.getPasswordHash())) {
                success = true;
                HttpSession session = request.getSession(true);
                session.setAttribute("username", user.getUsername());
                session.setAttribute("userId", user.getId());
                if ("Admin".equals(user.getUsername())) {
                    isAdmin = true;
                    session.setAttribute("isAdmin", true);
                } else {
                    session.removeAttribute("isAdmin");
                }
            }
        }

        // Persist every login request exactly once, regardless of success or failure.
        loginAttemptRepository.save(new LoginAttempt(normalizedEmail, loginUserId, success));

        String redirectTarget = resolveRedirectTarget(returnTo, request.getHeader("Referer"));
        if (success) {
            if (isAdmin) {
                return "redirect:/admin";
            }
            return "redirect:" + redirectTarget;
        }
        redirectAttributes.addFlashAttribute("error", "Invalid credentials");
        return "redirect:" + withAuthParam(redirectTarget, "login");
    }

    @PostMapping("/signup")
    public String doSignup(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(value = "returnTo", required = false) String returnTo,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        String redirectTarget = resolveRedirectTarget(returnTo, request.getHeader("Referer"));
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("signupError", "Username already exists");
            return "redirect:" + withAuthParam(redirectTarget, "signup");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("signupError", "Email already exists");
            return "redirect:" + withAuthParam(redirectTarget, "signup");
        }

        String hash = hash(password);
        User u = new User(username, normalizedEmail, hash);
        User saved = userRepository.save(u);
        HttpSession session = request.getSession(true);
        session.setAttribute("username", saved.getUsername());
        session.setAttribute("userId", saved.getId());
        session.removeAttribute("isAdmin");
        return "redirect:" + redirectTarget;
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    private String resolveRedirectTarget(String returnTo, String referer) {
        String candidate = normalizeReturnTo(returnTo);
        if (candidate != null) {
            return candidate;
        }
        candidate = normalizeReturnTo(referer);
        if (candidate != null) {
            return candidate;
        }
        return "/";
    }

    private String normalizeReturnTo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String candidate = value.trim();
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            java.net.URI uri = java.net.URI.create(candidate);
            candidate = uri.getPath();
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                candidate = candidate + "?" + uri.getQuery();
            }
        }

        if (!candidate.startsWith("/")) {
            return null;
        }

        if (candidate.startsWith("/login") || candidate.startsWith("/signup") || candidate.startsWith("/logout")) {
            return "/";
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(candidate).replaceQueryParam("auth");
        return builder.build(false).toUriString();
    }

    private String withAuthParam(String path, String authTab) {
        String safePath = normalizeReturnTo(path);
        if (safePath == null) {
            safePath = "/";
        }

        String encodedTab = UriUtils.encodeQueryParam(authTab, StandardCharsets.UTF_8);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(safePath).replaceQueryParam("auth");
        builder.queryParam("auth", encodedTab);
        return builder.build(false).toUriString();
    }
}