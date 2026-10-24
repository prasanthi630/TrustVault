
package com.trustvault.controller;

import com.trustvault.entity.DocumentShare;
import com.trustvault.service.DocumentShareService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
public class DocumentShareController {

    private final DocumentShareService documentShareService;

    public DocumentShareController(
            DocumentShareService documentShareService) {

        this.documentShareService = documentShareService;
    }

    // Shared Documents page
    @GetMapping("/shared-documents")
    public String sharedDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();

        model.addAttribute(
                "sharedDocuments",
                documentShareService.getSharedWithUser(username)
        );

        model.addAttribute(
                "myShares",
                documentShareService.getSharedByUser(username)
        );

        return "shared-documents";
    }

    // Share a document
    @PostMapping("/share-document")
    public String shareDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long documentId,
            @RequestParam String sharedWith,
            @RequestParam String permission) {

        String sharedBy = userDetails.getUsername();

        DocumentShare documentShare =
                documentShareService.shareDocument(
                        documentId,
                        sharedBy,
                        sharedWith,
                        permission
                );

        return "redirect:/shared-documents";
    }

    // Revoke sharing
    @PostMapping("/revoke-share")
    public String revokeShare(
            @RequestParam Long shareId) {

        documentShareService.revokeShare(shareId);

        return "redirect:/shared-documents";
    }
}
