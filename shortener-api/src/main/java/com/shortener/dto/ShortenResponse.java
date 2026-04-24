package com.shortener.dto;

public class ShortenResponse {

    private String shortCode;
    private String shortUrl;

    public ShortenResponse(String shortCode, String shortUrl) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
    }

    public String getShortCode() { return shortCode; }
    public String getShortUrl() { return shortUrl; }
}
