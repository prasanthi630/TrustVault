
package com.trustvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import com.trustvault.repository.DocumentRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.trustvault.entity.AppUser;
import com.trustvault.service.AppUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
@Controller
public class PageController {

    private final DocumentRepository documentRepository;
    private final AppUserService appUserService;

    public PageController(
            DocumentRepository documentRepository,
            AppUserService appUserService) {
        this.documentRepository = documentRepository;
        this.appUserService = appUserService;
    }

    
	

	@GetMapping("/")
	public String home() {
	    return "home";
	}
	@GetMapping("/home")
	public String homePage() {
	    return "home";
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
	                       Model model) {

	    AppUser user = appUserService.findByEmail(userDetails.getUsername());

	    model.addAttribute("name", user.getName());

	    return "dashboard";
	}
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/register")
    public String register() {
        return "register";
    }
    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute AppUser user,
            Model model) {

        try {
            appUserService.registerUser(user);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/documents")
    public String documents(Model model) {
        model.addAttribute("documents", documentRepository.findAll());
        return "documents";
    }
}
