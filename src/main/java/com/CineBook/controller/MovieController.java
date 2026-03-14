package com.CineBook.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import com.CineBook.repository.CarouselRepository;

@Controller
public class MovieController {
    @Autowired
    private CarouselRepository repository;

    @GetMapping("/")
    public String indexString(Model model) {
        try {
            model.addAttribute("carouselImages", repository.findAllProjectedBy());
        } catch (org.springframework.dao.DataAccessException ex) {
            ex.printStackTrace();
            model.addAttribute("carouselImages", java.util.Collections.emptyList());
        }
        return "index";
    }
    @GetMapping("/movie")
    public String movie() {
        return "movie";
    }

    @GetMapping("/tickets")
    public String tickets() {
        return "tickets";
    }

    @GetMapping("/seats")
    public String seats() {
        return "seats";
    }

    @GetMapping("/payment")
    public String payment() {
        return "payment";
    }

    @GetMapping("/success")
    public String success() {
        return "success";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}