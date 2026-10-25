
package com.trustvault.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.AppUserRepository;

@Controller
public class ProfileController {

    private final AppUserRepository appUserRepository;

    public ProfileController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    // Open edit profile page
    @GetMapping("/profile/edit")
    public String editProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        AppUser user = appUserRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);

        return "edit-profile";
    }

    // Save updated profile
    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Name cannot be empty.");
            return "redirect:/profile/edit";
        }

        if (name.trim().length() > 100) {
            redirectAttributes.addFlashAttribute(
                    "error", "Name must be 100 characters or fewer.");
            return "redirect:/profile/edit";
        }

        AppUser user = appUserRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        user.setName(name.trim());
        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "success", "Profile updated successfully!");

        return "redirect:/profile";
    }
}
