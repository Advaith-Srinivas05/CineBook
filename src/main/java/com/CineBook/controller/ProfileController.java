package com.CineBook.controller;

import com.CineBook.model.MovieBooking;
import com.CineBook.model.MovieRating;
import com.CineBook.model.User;
import com.CineBook.repository.MovieBookingRepository;
import com.CineBook.repository.MovieRatingRepository;
import com.CineBook.repository.UserRepository;
import com.CineBook.service.PasswordHashService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class ProfileController {

    private static final Path UPLOAD_DIR = Paths.get("src", "main", "resources", "static", "images", "uploads");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieBookingRepository movieBookingRepository;

    @Autowired
    private MovieRatingRepository movieRatingRepository;

    @Autowired
    private PasswordHashService passwordHashService;

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/?auth=login";
        }

        User user = userOpt.get();
        model.addAttribute("profileUser", user);
        model.addAttribute("profileImageUrl", GlobalUserModelAdvice.resolveProfileImageUrl(user.getProfileImage()));
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("username") String username,
                                @RequestParam("email") String email,
                                @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                @RequestParam(value = "location", required = false) String location,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/?auth=login";
        }

        User user = userOpt.get();
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.trim();
        String normalizedLocation = location == null ? "" : location.trim();

        if (normalizedUsername.isBlank()) {
            redirectAttributes.addFlashAttribute("profileError", "Username cannot be empty.");
            return "redirect:/profile";
        }

        Optional<User> byUsername = userRepository.findByUsername(normalizedUsername);
        if (byUsername.isPresent() && !byUsername.get().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("profileError", "Username is already taken.");
            return "redirect:/profile";
        }

        if (!normalizedEmail.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(normalizedEmail);
            if (byEmail.isPresent() && !byEmail.get().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("profileError", "Email is already in use.");
                return "redirect:/profile";
            }
        }

        if (!normalizedPhone.isBlank() && !normalizedPhone.matches("\\d{7,15}")) {
            redirectAttributes.addFlashAttribute("profileError", "Phone number must contain 7 to 15 digits.");
            return "redirect:/profile";
        }

        if (normalizedLocation.length() > 120) {
            redirectAttributes.addFlashAttribute("profileError", "Location is too long.");
            return "redirect:/profile";
        }

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail.isBlank() ? null : normalizedEmail);
        user.setPhoneNumber(normalizedPhone.isBlank() ? null : normalizedPhone);
        user.setLocation(normalizedLocation.isBlank() ? null : normalizedLocation);
        userRepository.save(user);

        session.setAttribute("username", user.getUsername());
        session.setAttribute("userId", user.getId());

        redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("profileImage") MultipartFile profileImage,
                                                                   HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please login to upload an image."));
        }

        if (profileImage == null || profileImage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select an image to upload."));
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed."));
        }

        String extension = resolveExtension(profileImage.getOriginalFilename(), contentType);
        String filename = "avatar-" + userOpt.get().getId() + "-" + UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path destination = UPLOAD_DIR.resolve(filename);
            Files.copy(profileImage.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            User user = userOpt.get();
            user.setProfileImage(filename);
            userRepository.save(user);

            Map<String, String> response = new LinkedHashMap<>();
            response.put("message", "Profile image updated successfully.");
            response.put("imageUrl", GlobalUserModelAdvice.resolveProfileImageUrl(filename));
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image. Please try again."));
        }
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/?auth=login";
        }

        User user = userOpt.get();
        String current = currentPassword == null ? "" : currentPassword;
        String next = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;

        if (next.length() < 8) {
            redirectAttributes.addFlashAttribute("passwordError", "New password must be at least 8 characters long.");
            return "redirect:/profile";
        }

        if (!next.equals(confirm)) {
            redirectAttributes.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/profile";
        }

        String currentHash = passwordHashService.hashSha256(current);
        if (currentHash == null || !currentHash.equals(user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("passwordError", "Current password is incorrect.");
            return "redirect:/profile";
        }

        String newHash = passwordHashService.hashSha256(next);
        if (newHash == null) {
            redirectAttributes.addFlashAttribute("passwordError", "Failed to update password.");
            return "redirect:/profile";
        }

        user.setPasswordHash(newHash);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("passwordSuccess", "Password updated successfully.");
        return "redirect:/profile";
    }

    @GetMapping("/booking-history")
    public String bookingHistory(Model model, HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/?auth=login";
        }

        List<MovieBooking> bookingHistory = movieBookingRepository.findHistoryByUserId(userOpt.get().getId());
        model.addAttribute("bookingHistory", bookingHistory);
        return "booking-history";
    }

    @GetMapping("/rating-history")
    public String ratingHistory(Model model, HttpSession session) {
        Optional<User> userOpt = getAuthenticatedUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/?auth=login";
        }

        List<MovieRating> ratingHistory = movieRatingRepository.findHistoryByUserId(userOpt.get().getId());
        model.addAttribute("ratingHistory", ratingHistory);
        return "rating-history";
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

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.(png|jpg|jpeg|gif|webp)")) {
                    return ext;
                }
            }
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".png";
    }

}
