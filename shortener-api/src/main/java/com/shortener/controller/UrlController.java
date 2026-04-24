package com.shortener.controller;

import com.shortener.dto.ShortenRequest;
import com.shortener.dto.ShortenResponse;
import com.shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@Tag(name = "URL Shortener", description = "Shorten and resolve URLs")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Shorten a URL")
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
        return urlService.shorten(request);
    }

    @GetMapping("/r/{code}")
    @Operation(summary = "Redirect to original URL")
    public RedirectView redirect(@PathVariable String code) {
        String originalUrl = urlService.resolve(code);
        return new RedirectView(originalUrl);
    }
}
