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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Optional<User> maybeUser = userRepository.findByUsername(username);
        boolean success = false;
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            String hash = hash(password);
            if (hash != null && hash.equals(user.getPasswordHash())) {
                success = true;
                HttpSession session = request.getSession(true);
                session.setAttribute("username", user.getUsername());
                if ("Admin".equals(user.getUsername())) {
                    session.setAttribute("isAdmin", true);
                    loginAttemptRepository.save(new LoginAttempt(user.getUsername(), true));
                    return "redirect:/admin";
                }
            }
        }
        loginAttemptRepository.save(new LoginAttempt(username, success));
        if (success) return "redirect:/";
        redirectAttributes.addFlashAttribute("error", "Invalid credentials");
        return "redirect:/?auth=login";
    }

    @PostMapping("/signup")
    public String doSignup(@RequestParam String username, @RequestParam String email, @RequestParam String password, RedirectAttributes redirectAttributes) {
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("signupError", "Username already exists");
            return "redirect:/?auth=signup";
        }
        String hash = hash(password);
        User u = new User(username, email, hash);
        userRepository.save(u);
        return "redirect:/?auth=login";
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
}