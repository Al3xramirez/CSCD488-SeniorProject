package com.cscd488seniorproject.syllabussyncproject.controllers;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cscd488seniorproject.syllabussyncproject.service.ProfilePhotoService;

/* Controller for handling profile photo upload and retrieval */
@RestController
@RequestMapping("/api/me")
public class ProfilePhotoController {
    
    // Service for handling profile photo logic
    private final ProfilePhotoService profilePhotoService;

    //Constructor, you know the drill
    public ProfilePhotoController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    //creates a new profile photo for user
    @PostMapping("/photo")
    public ResponseEntity<?> upload(Authentication auth, @RequestParam("photo") MultipartFile photo) {
        profilePhotoService.uploadMyPhoto(auth, photo);
        return ResponseEntity.ok("Photo uploaded");
    }

    //retrieves the current profile photo for the user, if it exists
    @GetMapping("/photo")
    public ResponseEntity<byte[]> get(Authentication auth) {
        
        ProfilePhotoService.PhotoPayload payload = profilePhotoService.getMyPhoto(auth);
        if (payload == null) {
            return ResponseEntity.noContent().build();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .cacheControl(CacheControl.noStore())
            .body(payload.bytes());
    }
}
