package com.CineBook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import com.CineBook.repository.CarouselRepository;
import com.CineBook.repository.MovieRepository;
import com.CineBook.model.Movie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
public class MovieController {
    @Autowired
    private CarouselRepository repository;

    @Autowired
    private MovieRepository movieRepository;

    @GetMapping("/")
    public String indexString(Model model, HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            return "redirect:/admin";
        }
        try {
            model.addAttribute("carouselImages", repository.findAllProjectedBy());
        } catch (org.springframework.dao.DataAccessException ex) {
            // ex.printStackTrace();
            model.addAttribute("carouselImages", java.util.Collections.emptyList());
        }
        return "index";
    }
    @GetMapping("/movie")
    public String movie(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "movie";
    }

    @GetMapping("/tickets")
    public String tickets(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "tickets";
    }

    @GetMapping("/seats")
    public String seats(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "seats";
    }

    @GetMapping("/payment")
    public String payment(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "payment";
    }

    @GetMapping("/success")
    public String success(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        return "success";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) return "redirect:/admin";
        if (username != null) return "redirect:/";
        return "login";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        Object username = session.getAttribute("username");
        if (isAdmin instanceof Boolean && (Boolean) isAdmin) {
            return "admin";
        }
        if (username != null) {
            return "redirect:/"; // logged-in non-admin users -> index
        }
        return "redirect:/login"; // not logged in -> login
    }

    @PostMapping("/admin/movies")
    public ResponseEntity<String> addMovie(@RequestParam("title") String title,
                                           @RequestParam("duration") Integer duration,
                                           @RequestParam("language") String language,
                                           @RequestParam("poster") MultipartFile poster,
                                           HttpSession session) {
        Object isAdmin = session.getAttribute("isAdmin");
        if (!(isAdmin instanceof Boolean && (Boolean) isAdmin)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        try {
            Movie m = new Movie();
            m.setTitle(title);
            m.setDurationMinutes(duration);
            m.setLanguage(language);
            if (poster != null && !poster.isEmpty()) {
                m.setPoster(poster.getBytes());
            }
            movieRepository.save(m);
            return ResponseEntity.ok("OK");
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Failed to read poster");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error");
        }
    }
}