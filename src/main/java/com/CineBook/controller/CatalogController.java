package com.CineBook.controller;

import com.CineBook.model.Movie;
import com.CineBook.model.Theater;
import com.CineBook.model.User;
import com.CineBook.repository.MovieRepository;
import com.CineBook.repository.TheaterRepository;
import com.CineBook.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CatalogController {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String login(HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ADMIN) return "redirect:/admin";
        if (userOpt.isPresent()) return "redirect:/";
        return "redirect:/?auth=login";
    }

    @GetMapping("/api/movies/search")
    public ResponseEntity<List<Map<String, Object>>> searchMovies(@RequestParam(value = "q", required = false) String q) {
        if (q == null) q = "";
        List<Movie> list = movieRepository.findTop10ByTitleContainingIgnoreCase(q);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Movie m : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            out.add(map);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/api/theaters/search")
    public ResponseEntity<List<Map<String, Object>>> searchTheaters(@RequestParam(value = "q", required = false) String q) {
        if (q == null) q = "";
        List<Theater> list = theaterRepository.findTop10ByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrLocationContainingIgnoreCase(q, q, q);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Theater t : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("city", t.getCity());
            map.put("location", t.getLocation());
            map.put("screenCount", t.getScreenCount());
            map.put("price", t.getPrice());
            map.put("elitePrice", t.getElitePrice());
            out.add(map);
        }
        return ResponseEntity.ok(out);
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
}
