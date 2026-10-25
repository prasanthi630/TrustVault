
package com.trustvault.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trustvault.service.AccountDeletionService;

@Controller
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    public AccountController(
            AccountDeletionService accountDeletionService) {
        this.accountDeletionService = accountDeletionService;
    }

    // Delete only the currently logged-in user's account
    @PostMapping("/profile/delete")
    public String deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        try {
            String email = userDetails.getUsername();

            accountDeletionService.deleteAccount(email, password);

            // Log the user out after successful deletion
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            new SecurityContextLogoutHandler()
                    .logout(request, response, authentication);

            return "redirect:/login?accountDeleted";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "deleteError", exception.getMessage());

            return "redirect:/profile";

        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute(
                    "deleteError",
                    "Account deletion could not finish. Please contact support.");

            return "redirect:/profile";
        }
    }
}
